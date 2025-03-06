// Copyright The OpenTelemetry Authors
// SPDX-License-Identifier: Apache-2.0
package kafka

import (
	"fmt"
	"os"

	"github.com/IBM/sarama"
	"github.com/sirupsen/logrus"
)

var (
	Topic = "orders"
)

func CreateKafkaProducer(log *logrus.Logger) (sarama.AsyncProducer, error) {
	// Retrieve Confluent Cloud configuration from environment variables
	broker := os.Getenv("CONFLUENT_BROKER")
	apiKey := os.Getenv("CONFLUENT_API_KEY")
	apiSecret := os.Getenv("CONFLUENT_API_SECRET")

	if broker == "" || apiKey == "" || apiSecret == "" {
		return nil, fmt.Errorf("Confluent Cloud configuration not found in environment variables. Please set CONFLUENT_BROKER, CONFLUENT_API_KEY, and CONFLUENT_API_SECRET")
	}

	// Sarama logger setup
	sarama.Logger = log

	// Sarama configuration
	config := sarama.NewConfig()
	config.Producer.Return.Successes = true
	config.Producer.Return.Errors = true
	config.Producer.RequiredAcks = sarama.WaitForAll // Ensure messages are acknowledged
	config.Version = sarama.V3_0_0_0

	// SASL configuration for Confluent Cloud
	config.Net.SASL.Enable = true
	config.Net.SASL.User = apiKey
	config.Net.SASL.Password = apiSecret
	config.Net.SASL.Mechanism = sarama.SASLTypePlaintext
	config.Net.TLS.Enable = true // Enable TLS for secure connection

	// Create the producer
	producer, err := sarama.NewAsyncProducer([]string{broker}, config)
	if err != nil {
		return nil, fmt.Errorf("failed to create Kafka producer: %w", err)
	}

	// Handle errors from the producer
	go func() {
		for err := range producer.Errors() {
			log.Errorf("Failed to produce message: %v", err)
		}
	}()

	log.Info("Kafka producer connected to Confluent Cloud")

	return producer, nil
}

