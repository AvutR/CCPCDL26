import csv

TOOLS = {"browser", "python", "sql", "shell"}

TRACE_FILE = "generated_trace.csv"


def parse_distribution(value):
    result = {}

    for item in value.split("|"):
        tool, probability = item.split(":")
        result[tool] = float(probability)

    return result


def main():
    errors = []

    with open(TRACE_FILE, newline="") as file:
        reader = csv.DictReader(file)

        for line_number, row in enumerate(reader, start=2):

            tenant = row["tenant_id"]
            timestep = int(row["timestep"])
            true_tool = row["true_next_tool"]
            weight = float(row["weight_w_i"])

            dist = parse_distribution(row["predicted_dist"])

            if true_tool not in TOOLS:
                errors.append(
                    f"Line {line_number}: invalid true tool"
                )

            if set(dist.keys()) != TOOLS:
                errors.append(
                    f"Line {line_number}: prediction tools mismatch"
                )

            probability_sum = sum(dist.values())

            if abs(probability_sum - 1.0) > 0.01:
                errors.append(
                    f"Line {line_number}: probabilities sum to "
                    f"{probability_sum}"
                )

            for tool, probability in dist.items():
                if probability < 0 or probability > 1:
                    errors.append(
                        f"Line {line_number}: invalid probability for {tool}"
                    )

            if weight <= 0:
                errors.append(
                    f"Line {line_number}: invalid tenant weight"
                )

            if timestep < 1:
                errors.append(
                    f"Line {line_number}: invalid timestep"
                )

    if errors:
        print("Validation failed:")

        for error in errors:
            print(error)

    else:
        print("Trace validation passed")


if __name__ == "__main__":
    main()
    