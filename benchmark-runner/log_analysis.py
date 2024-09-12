import re
import csv
import os
import glob
import pandas as pd
import matplotlib.pyplot as plt
from datetime import datetime, timedelta

# Log patterns for consumer and producer
log_patterns = {
    "consumer": r'Current Time: (.*?) \| Consume TPS: (\d+) \| AVG\(B2C\) RT\(ms\):\s+([\d\.]+) \| AVG\(S2C\) RT\(ms\):\s+([\d\.]+) \| MAX\(B2C\) RT\(ms\): (\d+) \| MAX\(S2C\) RT\(ms\): (\d+) \| Consume Fail: (\d+)',
    "producer": r'Current Time: (.*?) \| Send TPS: (\d+) \| Max RT\(ms\): (\d+) \| Average RT\(ms\):\s+([\d\.]+) \| Send Failed: (\d+) \| Response Failed: (\d+)'
}

output_files = {
    "consumer": ("consumer_performance_data.csv", "consumer_tps_chart.png", "consumer_avg_b2c_rt_chart.png", "consumer_avg_s2c_rt_chart.png", "consumer_max_b2c_rt_chart.png", "consumer_max_s2c_rt_chart.png"),
    "producer": ("producer_performance_data.csv", "producer_tps_chart.png", "producer_rt_chart.png")
}

def check_log_file(log_file):
    if not os.path.exists(log_file):
        print(f"Error: Log file {log_file} does not exist.")
        return False
    return True

def analyze_data(data, role):
    def parse_time(time_str):
        h, m, s = map(int, time_str.split(':'))
        return h * 3600 + m * 60 + s

    data["Elapsed Time (s)"] = data["Current Time"].apply(parse_time)  # Create elapsed time column

    data = data[data["Elapsed Time (s)"] > 30]

    if role == "producer":
        metrics = ["Send TPS", "Max RT (ms)", "Average RT (ms)"]
    else:
        metrics = ["Consume TPS", "AVG(B2C) RT (ms)", "AVG(S2C) RT (ms)", "MAX(B2C) RT (ms)", "MAX(S2C) RT (ms)"]

    for metric in metrics:
        data[metric] = pd.to_numeric(data[metric])

    result = {}
    for metric in metrics:
        result[metric] = (data[metric].min(), data[metric].max())
    
    return result

def write_benchmark_to_file(benchmark_data, role, output_file):
    with open(output_file, mode='w', newline='') as file:
        writer = csv.writer(file)
        writer.writerow(["Metric", "Min Value", "Max Value"])
        for metric, (min_value, max_value) in benchmark_data.items():  # Expecting (min, max) tuple
            writer.writerow([metric, min_value, max_value])

# Parse log file and filter out the first 30 seconds
def parse_log_file(log_file, pattern, role):
    data = []
    first_timestamp = None
    try:
        with open(log_file, 'r') as file:
            for line in file:
                match = re.search(pattern, line)
                if match:
                    if role == "consumer":
                        log_time, tps, avg_b2c_rt, avg_s2c_rt, max_b2c_rt, max_s2c_rt, consume_fail = match.groups()
                        data_tuple = (log_time, tps, avg_b2c_rt, avg_s2c_rt, max_b2c_rt, max_s2c_rt, consume_fail)
                    else:
                        log_time, tps, max_rt, avg_rt, send_failed, response_failed = match.groups()
                        data_tuple = (log_time, tps, max_rt, avg_rt, send_failed, response_failed)

                    time_only = log_time.split()[1].split(',')[0]
                    log_datetime = datetime.strptime(time_only, "%H:%M:%S")

                    if first_timestamp is None:
                        first_timestamp = log_datetime
                    time_diff = log_datetime - first_timestamp

                    # Skip data from the first 30 seconds
                    if time_diff > timedelta(seconds=30):
                        data.append((time_only, *data_tuple[1:]))
    except FileNotFoundError as e:
        print(f"Error: {e}")
        return []
    return data

# Plot and save charts
def plot_charts(data, columns, title, ylabel, output_file, x_column='Current Time'):
    plt.figure(figsize=(10, 6))
    plt.plot(data[x_column], data[columns], label=title)
    plt.xticks(rotation=45)
    plt.gca().xaxis.set_major_locator(plt.MaxNLocator(10))
    plt.xlabel('Time (HH:MM:SS)')
    plt.ylabel(ylabel)
    plt.title(f'{title} Over Time')
    plt.legend()
    plt.grid(True)
    plt.tight_layout()
    plt.savefig(output_file)
    print(f"{title} chart saved as {output_file}")

# Process consumer and producer logs
def process_log(log_role):
    log_file_pattern = f'./{log_role}*.log'
    log_file = glob.glob(log_file_pattern)
    if log_file:
        log_file = log_file[0]
    else:
        print(f"Error: No log file found matching pattern '{log_file_pattern}'.")
        exit(1)

    if not check_log_file(log_file):
        print("Exiting program due to missing log file.")
        exit(1)

    pattern = log_patterns[log_role]
    csv_output_file, tps_image_output_file, *extra_images = output_files[log_role]

    log_data = parse_log_file(log_file, pattern, log_role)

    with open(csv_output_file, mode='w', newline='') as file:
        writer = csv.writer(file)
        if log_role == "consumer":
            writer.writerow(["Current Time", "Consume TPS", "AVG(B2C) RT (ms)", "AVG(S2C) RT (ms)", "MAX(B2C) RT (ms)", "MAX(S2C) RT (ms)", "Consume Fail"])
        else:
            writer.writerow(["Current Time", "Send TPS", "Max RT (ms)", "Average RT (ms)", "Send Failed", "Response Failed"])
        writer.writerows(log_data)
    print(f"Data written to {csv_output_file}.")

    df = pd.read_csv(csv_output_file)

    # Plot TPS and RT charts
    plot_charts(df, 'Send TPS' if log_role == 'producer' else 'Consume TPS', f"{log_role.capitalize()} TPS", 'TPS', tps_image_output_file)
    
    if log_role == "producer":
        plot_charts(df, 'Average RT (ms)', f"{log_role.capitalize()} Average RT", 'RT (ms)', extra_images[0])
    else:
        plot_charts(df, 'AVG(B2C) RT (ms)', f"{log_role.capitalize()} AVG(B2C) RT", 'RT (ms)', extra_images[0])
        plot_charts(df, 'AVG(S2C) RT (ms)', f"{log_role.capitalize()} AVG(S2C) RT", 'RT (ms)', extra_images[1])
        plot_charts(df, 'MAX(S2C) RT (ms)', f"{log_role.capitalize()} MAX(S2C) RT", 'RT (ms)', extra_images[2])
        plot_charts(df, 'MAX(B2C) RT (ms)', f"{log_role.capitalize()} MAX(B2C) RT", 'RT (ms)', extra_images[3])

    # Analyze and write benchmark results
    benchmark = analyze_data(df, log_role)
    write_benchmark_to_file(benchmark, log_role, f"{log_role}_benchmark_result.csv")
    print(f"Benchmark data written to {log_role}_benchmark_result.csv")

process_log("consumer")
process_log("producer")
