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
package io.trino.plugin.hive;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.trino.plugin.hive.acid.AcidTransaction;
import io.trino.plugin.hive.metastore.HivePageSinkMetadata;
import io.trino.spi.connector.ConnectorOutputTableHandle;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public class HiveOutputTableHandle
        extends HiveWritableTableHandle
        implements ConnectorOutputTableHandle
{
    private final List<String> partitionedBy;
    private final String tableOwner;
    private final Map<String, String> additionalTableParameters;
    private final boolean external;

    @JsonCreator
    public HiveOutputTableHandle(
            @JsonProperty("schemaName") String schemaName,
            @JsonProperty("tableName") String tableName,
            @JsonProperty("inputColumns") List<HiveColumnHandle> inputColumns,
            @JsonProperty("pageSinkMetadata") HivePageSinkMetadata pageSinkMetadata,
            @JsonProperty("locationHandle") LocationHandle locationHandle,
            @JsonProperty("tableStorageFormat") HiveStorageFormat tableStorageFormat,
            @JsonProperty("partitionStorageFormat") HiveStorageFormat partitionStorageFormat,
            @JsonProperty("partitionedBy") List<String> partitionedBy,
            @JsonProperty("bucketInfo") Optional<BucketInfo> bucketInfo,
            @JsonProperty("tableOwner") String tableOwner,
            @JsonProperty("additionalTableParameters") Map<String, String> additionalTableParameters,
            @JsonProperty("transaction") AcidTransaction transaction,
            @JsonProperty("external") boolean external,
            @JsonProperty("retriesEnabled") boolean retriesEnabled)
    {
        super(
                schemaName,
                tableName,
                inputColumns,
                pageSinkMetadata,
                locationHandle,
                bucketInfo,
                tableStorageFormat,
                partitionStorageFormat,
                transaction,
                retriesEnabled);

        this.partitionedBy = ImmutableList.copyOf(requireNonNull(partitionedBy, "partitionedBy is null"));
        this.tableOwner = requireNonNull(tableOwner, "tableOwner is null");
        this.additionalTableParameters = ImmutableMap.copyOf(requireNonNull(additionalTableParameters, "additionalTableParameters is null"));
        this.external = external;
    }

    @JsonProperty
    public List<String> getPartitionedBy()
    {
        return partitionedBy;
    }

    @JsonProperty
    public String getTableOwner()
    {
        return tableOwner;
    }

    @JsonProperty
    public Map<String, String> getAdditionalTableParameters()
    {
        return additionalTableParameters;
    }

    @JsonProperty
    public boolean isExternal()
    {
        return external;
    }
}
