/*
 * Copyright (c) 2019-2020 VMware, Inc. or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package reactor.rabbitmq;

import com.rabbitmq.client.Connection;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import reactor.core.publisher.Flux;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 1, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 1, time = 5, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@Threads(2)
public class SenderMaxInFlightBenchmark {

    Connection connection;
    Sender sender;
    String queue;
    Flux<OutboundMessage> msgFlux;

    @Param({"1", "10", "100", "1000"})
    public int nbMessages;

    @Param({"1", "256", Integer.MAX_VALUE + ""})
    public int maxInFlight;

    @Setup
    public void setupConnection() throws Exception {
        connection = SenderBenchmarkUtils.newConnection();
    }

    @TearDown
    public void closeConnection() throws Exception {
        connection.close();
    }

    @Setup(Level.Iteration)
    public void setupSender() throws Exception {
        queue = SenderBenchmarkUtils.declareQueue(connection);
        sender = RabbitFlux.createSender();
        msgFlux = SenderBenchmarkUtils.outboundMessageFlux(queue, nbMessages);
    }

    @TearDown(Level.Iteration)
    public void tearDownSender() throws Exception {
        SenderBenchmarkUtils.deleteQueue(connection, queue);
        if (sender != null) {
            sender.close();
        }
    }

    @Benchmark
    public void sendWithPublishConfirmsMaxInFlight(Blackhole blackhole) {
        blackhole.consume(sender.sendWithPublishConfirms(msgFlux, new SendOptions().maxInFlight(maxInFlight)).blockLast());
    }

}
