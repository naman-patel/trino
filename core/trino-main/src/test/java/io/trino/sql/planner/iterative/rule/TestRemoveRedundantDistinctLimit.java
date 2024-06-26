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
package io.trino.sql.planner.iterative.rule;

import com.google.common.collect.ImmutableList;
import io.trino.sql.planner.iterative.rule.test.BaseRuleTest;
import io.trino.sql.planner.plan.AggregationNode;
import io.trino.sql.planner.plan.ValuesNode;
import org.junit.jupiter.api.Test;

import static io.trino.sql.planner.assertions.PlanMatchPattern.node;

public class TestRemoveRedundantDistinctLimit
        extends BaseRuleTest
{
    @Test
    public void test()
    {
        tester().assertThat(new RemoveRedundantDistinctLimit())
                .on(p ->
                        p.distinctLimit(
                                10,
                                ImmutableList.of(p.symbol("c")),
                                p.values(1, p.symbol("c"))))
                .matches(node(ValuesNode.class));

        tester().assertThat(new RemoveRedundantDistinctLimit())
                .on(p ->
                        p.distinctLimit(
                                10,
                                ImmutableList.of(p.symbol("c")),
                                p.values(6, p.symbol("c"))))
                .matches(
                        node(AggregationNode.class,
                                node(ValuesNode.class)));

        tester().assertThat(new RemoveRedundantDistinctLimit())
                .on(p ->
                        p.distinctLimit(
                                0,
                                ImmutableList.of(p.symbol("c")),
                                p.values(1, p.symbol("c"))))
                .matches(node(ValuesNode.class));
    }

    @Test
    public void doesNotFire()
    {
        tester().assertThat(new RemoveRedundantDistinctLimit())
                .on(p ->
                        p.distinctLimit(
                                10,
                                ImmutableList.of(p.symbol("c")),
                                p.values(100, p.symbol("c"))))
                .doesNotFire();
    }
}
