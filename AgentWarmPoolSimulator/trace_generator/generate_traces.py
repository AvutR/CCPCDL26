import csv
import random

TOOLS = ["browser", "python", "sql", "shell"]

NUM_TENANTS = 3
NUM_TIMESTEPS = 20

PREDICTION_ACCURACY = 0.8
BURST_CORRELATION = 0.0

OUTPUT_FILE = "generated_trace.csv"

random.seed(42)


def generate_prediction(true_tool):
    prediction_correct = random.random() < PREDICTION_ACCURACY

    if prediction_correct:
        predicted_top_tool = true_tool
    else:
        predicted_top_tool = random.choice(
            [tool for tool in TOOLS if tool != true_tool]
        )

    top_probability = random.uniform(0.60, 0.80)

    remaining_probability = 1.0 - top_probability
    other_tools = [tool for tool in TOOLS if tool != predicted_top_tool]

    values = [random.random() for _ in other_tools]
    total = sum(values)

    prediction = {
        predicted_top_tool: top_probability
    }

    for tool, value in zip(other_tools, values):
        prediction[tool] = remaining_probability * value / total

    return prediction


def encode_prediction(prediction):
    return "|".join(
        f"{tool}:{prediction[tool]:.4f}"
        for tool in TOOLS
    )


def main():
    rows = []

    for timestep in range(1, NUM_TIMESTEPS + 1):

        burst_active = random.random() < BURST_CORRELATION
        burst_tool = random.choice(TOOLS) if burst_active else None

        for tenant_num in range(1, NUM_TENANTS + 1):

            tenant_id = f"T{tenant_num}"

            if burst_active:
                true_tool = burst_tool
            else:
                true_tool = random.choice(TOOLS)

            prediction = generate_prediction(true_tool)

            rows.append({
                "tenant_id": tenant_id,
                "timestep": timestep,
                "true_next_tool": true_tool,
                "predicted_dist": encode_prediction(prediction),
                "weight_w_i": 1.0
            })

    with open(OUTPUT_FILE, "w", newline="") as file:
        writer = csv.DictWriter(
            file,
            fieldnames=[
                "tenant_id",
                "timestep",
                "true_next_tool",
                "predicted_dist",
                "weight_w_i"
            ]
        )

        writer.writeheader()
        writer.writerows(rows)

    print(f"Generated {len(rows)} rows")
    print(f"Saved to {OUTPUT_FILE}")


if __name__ == "__main__":
    main()