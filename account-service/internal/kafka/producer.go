package kafka

import (
	"context"
	"encoding/json"
	"fmt"
	"net"
	"strconv"
	"strings"
	"time"

	"github.com/segmentio/kafka-go"
)

// Producer - Зависимости на продюсер и топик
type Producer struct {
	writer *kafka.Writer
	topic  string
}

// Config - конфиг продюсера
type Config struct {
	Brokers           []string
	Topic             string
	NumPartitions     int
	ReplicationFactor int
}

// NewProducer - создает продюсер и топик (если нужно), вернет объект продюсера
func NewProducer(ctx context.Context, cfg Config) (*Producer, error) {
	// Проверяем конфиг
	if len(cfg.Brokers) == 0 {
		return nil, fmt.Errorf("no kafka brokers")
	}
	if cfg.Topic == "" {
		return nil, fmt.Errorf("topic is required")
	}

	// Значения по умолчанию
	if cfg.NumPartitions == 0 {
		cfg.NumPartitions = 3
	}
	if cfg.ReplicationFactor == 0 {
		cfg.ReplicationFactor = 1
	}

	// Если нет топика - создаем через контроллер
	if err := ensureTopic(ctx, cfg.Brokers, cfg.Topic, cfg.NumPartitions, cfg.ReplicationFactor); err != nil {
		return nil, fmt.Errorf("ensure topic: %w", err)
	}

	// Создаём writer. Async=false => WriteMessages будет синхронно возвращать ошибку (без потери ошибок).
	writer := kafka.NewWriter(kafka.WriterConfig{
		Brokers:  cfg.Brokers,
		Topic:    cfg.Topic,
		Balancer: &kafka.LeastBytes{},
		Async:    false,
	})

	return &Producer{
		writer: writer,
		topic:  cfg.Topic,
	}, nil
}

// ensureTopic - подключаемся к контроллеру и создаем топик, если его нет
func ensureTopic(ctx context.Context, brokers []string, topic string, partitions, replication int) error {
	var dialErr error
	for _, broker := range brokers {
		// Подключаемся к брокеру, чтобы узнать controller
		conn, err := kafka.DialContext(ctx, "tcp", broker)
		if err != nil {
			dialErr = err
			continue
		}
		// Не defer здесь в цикле (закрываем сразу), чтобы не держать лишние соединения
		conn.Close()

		// Получим контроллер информации через отдельное соединение
		conn2, err := kafka.DialContext(ctx, "tcp", broker)
		if err != nil {
			dialErr = err
			continue
		}

		controller, err := conn2.Controller()
		conn2.Close()
		if err != nil {
			return fmt.Errorf("could not get kafka controller: %w", err)
		}

		controllerAddr := net.JoinHostPort(controller.Host, strconv.Itoa(controller.Port))
		controllerConn, err := kafka.DialContext(ctx, "tcp", controllerAddr)
		if err != nil {
			return fmt.Errorf("could not dial kafka controller: %w", err)
		}
		defer controllerConn.Close()

		configs := []kafka.TopicConfig{
			{
				Topic:             topic,
				NumPartitions:     partitions,
				ReplicationFactor: replication,
			},
		}
		if err := controllerConn.CreateTopics(configs...); err != nil {
			// Если ошибка — проверим, существует ли топик
			partitionsInfo, perr := controllerConn.ReadPartitions()
			if perr != nil {
				return fmt.Errorf("create topics error: %w (and read partitions failed: %v)", err, perr)
			}
			for _, p := range partitionsInfo {
				if p.Topic == topic {
					// Топик уже существует - значит всё норм
					return nil
				}
			}
			return fmt.Errorf("create topics returned error and topic not found: %w", err)
		}

		// Успешно создали или не получили ошибку
		return nil
	}

	return fmt.Errorf("dial to brokers failed: %v", dialErr)
}

// PublishJSON отправляет произвольный JSON payload в топик с ключом.
func (p *Producer) PublishJSON(ctx context.Context, key string, v interface{}) error {
	b, err := json.Marshal(v)
	if err != nil {
		return fmt.Errorf("marshal message: %w", err)
	}

	msg := kafka.Message{
		Key:   []byte(key),
		Value: b,
		Time:  time.Now(),
	}
	if err := p.writer.WriteMessages(ctx, msg); err != nil {
		return fmt.Errorf("write message: %w", err)
	}
	return nil
}

// Close - закрывает писателя
func (p *Producer) Close() error {
	return p.writer.Close()
}

// ParseBrokers - вспомогательная функция чтобы распарсить брокеры
func ParseBrokers(brokersStr string) []string {
	var out []string
	for _, s := range strings.Split(brokersStr, ",") {
		trim := strings.TrimSpace(s)
		if trim != "" {
			out = append(out, trim)
		}
	}
	return out
}
