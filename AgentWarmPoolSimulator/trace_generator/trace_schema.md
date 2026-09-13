
# Trace Schema

## trace CSV

Columns:

tenant_id
timestep
true_next_tool
predicted_dist
weight_w_i

Example:

T1,1,browser,"browser:0.7|python:0.2|sql:0.1|shell:0.0",1.0

Rules:

- tenant_id: string such as T1, T2, T3
- timestep: integer starting from 1
- true_next_tool: must exist in tools.csv
- predicted_dist: format is tool:prob|tool:prob|...
- all tool probabilities must sum to 1
- weight_w_i: positive float

## tools CSV

Columns:

tool_id
mem_mb
image_pull_ms
runtime_init_ms
exec_duration_ms

Units:

- memory = MB
- all time values = milliseconds