import re
import csv
import os
import glob
import pandas as pd
import matplotlib.pyplot as plt
from datetime import datetime, timedelta

producer_log_file = glob.glob('./producer*.log')
if producer_log_file:
    producer_log_file = producer_log_file[0] 
else:
    print("Error: No log file found matching pattern './producer*.log'.")
    exit(1)

csv_output_file = 'producer_performance_data.csv'
image_output_file = 'producer_performance_chart.png' 

log_pattern = r'Current Time: (.*?) \| Send TPS: (\d+) \| Max RT\(ms\): (\d+) \| Average RT\(ms\):\s+([\d\.]+) \| Send Failed: (\d+) \| Response Failed: (\d+)'

def check_log_files(*log_files):
    for log_file in log_files:
        if not os.path.exists(log_file):
            print(f"Error: Log file {log_file} does not exist.")
            return False
    return True

def parse_log_file(log_file):
    data = []
    first_timestamp = None
    try:
        with open(log_file, 'r') as file:
            for line in file:
                match = re.search(log_pattern, line)
                if match:
                    log_time, tps, max_rt, avg_rt, send_failed, response_failed = match.groups()
                    time_only = log_time.split()[1].split(',')[0]
                    log_datetime = datetime.strptime(time_only, "%H:%M:%S")
                    
                    if first_timestamp is None:
                        first_timestamp = log_datetime
                    time_diff = log_datetime - first_timestamp

                    if time_diff > timedelta(seconds=30):
                        data.append((time_only, tps, max_rt, avg_rt, send_failed, response_failed))
    except FileNotFoundError as e:
        print(f"Error: {e}")
        return []
    return data

if not check_log_files(producer_log_file):
    print("Exiting program due to missing log file.")
    exit(1)

producer_data = parse_log_file(producer_log_file)

# Write data to CSV file
with open(csv_output_file, mode='w', newline='') as file:
    writer = csv.writer(file)
    writer.writerow(["Current Time", "Send TPS", "Max RT (ms)", "Average RT (ms)", "Send Failed", "Response Failed", "Source"])
    
    for row in producer_data:
        writer.writerow(list(row) + ['Producer'])

print(f"Data successfully written to {csv_output_file}")

data = pd.read_csv(csv_output_file)

plt.figure(figsize=(12, 6))

# Plot Producer TPS chart
plt.subplot(1, 2, 1)
producer_data = data[data['Source'] == 'Producer']
plt.plot(producer_data['Current Time'], producer_data['Send TPS'], label='Producer TPS')
plt.xticks(rotation=45)  
plt.gca().xaxis.set_major_locator(plt.MaxNLocator(10))  
plt.xlabel('Time (HH:MM:SS)')
plt.ylabel('TPS')
plt.title('Producer TPS Over Time')
plt.legend()
plt.grid(True)

# Plot Producer Average RT chart
plt.subplot(1, 2, 2)
plt.plot(producer_data['Current Time'], producer_data['Average RT (ms)'], label='Producer Average RT', color='red')
plt.xticks(rotation=45)  
plt.gca().xaxis.set_major_locator(plt.MaxNLocator(10))
plt.xlabel('Time (HH:MM:SS)')
plt.ylabel('Response Time (ms)')
plt.title('Producer Average RT Over Time')
plt.legend()
plt.grid(True)

plt.tight_layout()

plt.savefig(image_output_file)

print(f"Chart saved as {image_output_file}")
