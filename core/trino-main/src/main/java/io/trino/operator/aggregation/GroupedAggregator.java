/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.trino.operator.aggregation;

import com.google.common.primitives.Ints;
import io.trino.operator.AggregationMetrics;
import io.trino.spi.Page;
import io.trino.spi.block.Block;
import io.trino.spi.block.BlockBuilder;
import io.trino.spi.type.Type;
import io.trino.sql.planner.plan.AggregationNode;
import io.trino.sql.planner.plan.AggregationNode.Step;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

public class GroupedAggregator
{
    private final GroupedAccumulator accumulator;
    private AggregationNode.Step step;
    private final Type intermediateType;
    private final Type finalType;
    private final int[] inputChannels;
    private final OptionalInt maskChannel;
    private final AggregationMaskBuilder maskBuilder;
    private final AggregationMetrics metrics;

    public GroupedAggregator(
            GroupedAccumulator accumulator,
            Step step,
            Type intermediateType,
            Type finalType,
            List<Integer> inputChannels,
            OptionalInt maskChannel,
            AggregationMaskBuilder maskBuilder,
            AggregationMetrics metrics)
    {
        this.accumulator = requireNonNull(accumulator, "accumulator is null");
        this.step = requireNonNull(step, "step is null");
        this.intermediateType = requireNonNull(intermediateType, "intermediateType is null");
        this.finalType = requireNonNull(finalType, "finalType is null");
        this.inputChannels = Ints.toArray(requireNonNull(inputChannels, "inputChannels is null"));
        this.maskChannel = requireNonNull(maskChannel, "maskChannel is null");
        this.maskBuilder = requireNonNull(maskBuilder, "maskBuilder is null");
        this.metrics = requireNonNull(metrics, "metrics is null");
        checkArgument(step.isInputRaw() || inputChannels.size() == 1, "expected 1 input channel for intermediate aggregation");
    }

    public long getEstimatedSize()
    {
        return accumulator.getEstimatedSize();
    }

    public Type getType()
    {
        if (step.isOutputPartial()) {
            return intermediateType;
        }
        return finalType;
    }

    public void processPage(int groupCount, int[] groupIds, Page page)
    {
        accumulator.setGroupCount(groupCount);

        if (step.isInputRaw()) {
            Page arguments = page.getColumns(inputChannels);
            Optional<Block> maskBlock = Optional.empty();
            if (maskChannel.isPresent()) {
                maskBlock = Optional.of(page.getBlock(maskChannel.getAsInt()));
            }
            AggregationMask mask = maskBuilder.buildAggregationMask(arguments, maskBlock);

            if (mask.isSelectNone()) {
                return;
            }
            long start = System.nanoTime();
            accumulator.addInput(groupIds, arguments, mask);
            metrics.recordAccumulatorUpdateTimeSince(start);
        }
        else {
            long start = System.nanoTime();
            accumulator.addIntermediate(groupIds, page.getBlock(inputChannels[0]));
            metrics.recordAccumulatorUpdateTimeSince(start);
        }
    }

    public void prepareFinal()
    {
        accumulator.prepareFinal();
    }

    public void evaluate(int groupId, BlockBuilder output)
    {
        if (step.isOutputPartial()) {
            accumulator.evaluateIntermediate(groupId, output);
        }
        else {
            accumulator.evaluateFinal(groupId, output);
        }
    }

    // todo this should return a new GroupedAggregator instead of modifying the existing object
    public void setSpillOutput()
    {
        step = AggregationNode.Step.partialOutput(step);
    }

    public Type getSpillType()
    {
        return intermediateType;
    }
}
