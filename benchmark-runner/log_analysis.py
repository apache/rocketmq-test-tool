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

# Check if log file exists
def check_log_file(log_file):
    if not os.path.exists(log_file):
        print(f"Error: Log file {log_file} does not exist.")
        return False
    return True

# Function to parse log file and filter out the first 30 seconds
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

    # Write data to CSV file
    with open(csv_output_file, mode='w', newline='') as file:
        writer = csv.writer(file)
        if log_role == "consumer":
            writer.writerow(["Current Time", "Consume TPS", "AVG(B2C) RT (ms)", "AVG(S2C) RT (ms)", "MAX(B2C) RT (ms)", "MAX(S2C) RT (ms)", "Consume Fail", "Source"])
        else:
            writer.writerow(["Current Time", "Send TPS", "Max RT (ms)", "Average RT (ms)", "Send Failed", "Response Failed", "Source"])
        
        for row in log_data:
            writer.writerow(list(row) + [log_role.capitalize()])

    print(f"Data successfully written to {csv_output_file}")

    data = pd.read_csv(csv_output_file)

    # Plot charts based on role
    if log_role == "consumer":
        plot_charts(data[data['Source'] == 'Consumer'], 'Consume TPS', 'Consumer TPS', 'TPS', tps_image_output_file)
        plot_charts(data[data['Source'] == 'Consumer'], 'AVG(B2C) RT (ms)', 'Consumer AVG(B2C) RT', 'Response Time (ms)', extra_images[0])
        plot_charts(data[data['Source'] == 'Consumer'], 'AVG(S2C) RT (ms)', 'Consumer AVG(S2C) RT', 'Response Time (ms)', extra_images[1])
        plot_charts(data[data['Source'] == 'Consumer'], 'MAX(S2C) RT (ms)', 'Consumer MAX(S2C) RT', 'Response Time (ms)', extra_images[2])
        plot_charts(data[data['Source'] == 'Consumer'], 'MAX(B2C) RT (ms)', 'Consumer MAX(B2C) RT', 'Response Time (ms)', extra_images[3])
    else:
        plot_charts(data[data['Source'] == 'Producer'], 'Send TPS', 'Producer TPS', 'TPS', tps_image_output_file)
        plot_charts(data[data['Source'] == 'Producer'], 'Average RT (ms)', 'Producer Average RT', 'Response Time (ms)', extra_images[0])

process_log('consumer')
process_log('producer')
