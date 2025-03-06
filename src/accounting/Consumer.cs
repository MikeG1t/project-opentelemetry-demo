// Copyright The OpenTelemetry Authors
// SPDX-License-Identifier: Apache-2.0

using Confluent.Kafka;
using Microsoft.Extensions.Logging;
using Oteldemo;

namespace Accounting;

internal class Consumer : IDisposable
{
    private const string TopicName = "orders";

    private ILogger _logger;
    private IConsumer<string, byte[]> _consumer;
    private bool _isListening;

    public Consumer(ILogger<Consumer> logger)
    {
        _logger = logger;

        var servers = Environment.GetEnvironmentVariable("CONFLUENT_BROKER")
            ?? throw new ArgumentNullException("CONFLUENT_BROKER");
        var apiKey = Environment.GetEnvironmentVariable("CONFLUENT_API_KEY")
            ?? throw new ArgumentNullException("CONFLUENT_API_KEY");
        var apiSecret = Environment.GetEnvironmentVariable("CONFLUENT_API_SECRET")
            ?? throw new ArgumentNullException("CONFLUENT_API_SECRET");

        _consumer = BuildConsumer(servers, apiKey, apiSecret);
        _consumer.Subscribe(TopicName);

        _logger.LogInformation($"Connecting to Confluent Cloud Kafka: {servers}");
    }

    public void StartListening()
    {
        _isListening = true;

        try
        {
            while (_isListening)
            {
                try
                {
                    var consumeResult = _consumer.Consume();

                    ProcessMessage(consumeResult.Message);
                }
                catch (ConsumeException e)
                {
                    _logger.LogError(e, "Consume error: {0}", e.Error.Reason);
                }
            }
        }
        catch (OperationCanceledException)
        {
            _logger.LogInformation("Closing consumer");

            _consumer.Close();
        }
    }

    private void ProcessMessage(Message<string, byte[]> message)
    {
        try
        {
            var order = OrderResult.Parser.ParseFrom(message.Value);

            Log.OrderReceivedMessage(_logger, order);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Order parsing failed:");
        }
    }

    private IConsumer<string, byte[]> BuildConsumer(string servers, string apiKey, string apiSecret)
    {
        var conf = new ConsumerConfig
        {
            GroupId = $"accounting",
            BootstrapServers = servers,
            AutoOffsetReset = AutoOffsetReset.Earliest,
            EnableAutoCommit = true,
            SecurityProtocol = SecurityProtocol.SaslSsl,
            SaslMechanism = SaslMechanism.Plain,
            SaslUsername = apiKey,
            SaslPassword = apiSecret
        };

        return new ConsumerBuilder<string, byte[]>(conf)
            .Build();
    }

    public void Dispose()
    {
        _isListening = false;
        _consumer?.Dispose();
    }
}
