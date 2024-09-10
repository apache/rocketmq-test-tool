import re
import csv
import os
import glob
import pandas as pd
import matplotlib.pyplot as plt
from datetime import datetime, timedelta

# Define log file path
consumer_log_file = glob.glob('./consumer*.log')
if consumer_log_file:
    consumer_log_file = consumer_log_file[0]  
else:
    print("Error: No log file found matching pattern './consumer*.log'.")
    exit(1)

csv_output_file = 'consumer_performance_data.csv'
image_output_file = 'consumer_performance_chart.png' 

# Regular expression for matching the log content
log_pattern = r'Current Time: (.*?) \| Consume TPS: (\d+) \| AVG\(B2C\) RT\(ms\):\s+([\d\.]+) \| AVG\(S2C\) RT\(ms\):\s+([\d\.]+) \| MAX\(B2C\) RT\(ms\): (\d+) \| MAX\(S2C\) RT\(ms\): (\d+) \| Consume Fail: (\d+)'

def check_log_file(log_file):
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
                    log_time, tps, avg_b2c_rt, avg_s2c_rt, max_b2c_rt, max_s2c_rt, consume_fail = match.groups()
                    time_only = log_time.split()[1].split(',')[0]
                    log_datetime = datetime.strptime(time_only, "%H:%M:%S")
                    
                    if first_timestamp is None:
                        first_timestamp = log_datetime
                    time_diff = log_datetime - first_timestamp

                    # Skip data from the first 30 seconds
                    if time_diff > timedelta(seconds=30):
                        data.append((time_only, tps, avg_b2c_rt, avg_s2c_rt, max_b2c_rt, max_s2c_rt, consume_fail))
    except FileNotFoundError as e:
        print(f"Error: {e}")
        return []
    return data

if not check_log_file(consumer_log_file):
    print("Exiting program due to missing log file.")
    exit(1)

consumer_data = parse_log_file(consumer_log_file)

# Write data to CSV file
with open(csv_output_file, mode='w', newline='') as file:
    writer = csv.writer(file)
    writer.writerow(["Current Time", "Consume TPS", "AVG(B2C) RT (ms)", "AVG(S2C) RT (ms)", "MAX(B2C) RT (ms)", "MAX(S2C) RT (ms)", "Consume Fail", "Source"])
    
    for row in consumer_data:
        writer.writerow(list(row) + ['Consumer'])

print(f"Data successfully written to {csv_output_file}")

data = pd.read_csv(csv_output_file)

plt.figure(figsize=(12, 6))

# Plot Consumer TPS chart
plt.subplot(1, 2, 1)
consumer_data = data[data['Source'] == 'Consumer']
plt.plot(consumer_data['Current Time'], consumer_data['Consume TPS'], label='Consumer TPS')
plt.xticks(rotation=45)  # Rotate the x-axis labels for better readability
plt.gca().xaxis.set_major_locator(plt.MaxNLocator(10))  # Limit the number of x-axis ticks
plt.xlabel('Time (HH:MM:SS)')
plt.ylabel('TPS')
plt.title('Consumer TPS Over Time')
plt.legend()
plt.grid(True)

# Plot Consumer AVG(B2C) RT chart
plt.subplot(1, 2, 2)
plt.plot(consumer_data['Current Time'], consumer_data['AVG(B2C) RT (ms)'], label='Consumer AVG(B2C) RT', color='red')
plt.xticks(rotation=45) 
plt.gca().xaxis.set_major_locator(plt.MaxNLocator(10)) 
plt.xlabel('Time (HH:MM:SS)')
plt.ylabel('Response Time (ms)')
plt.title('Consumer AVG(B2C) RT Over Time')
plt.legend()
plt.grid(True)

# Adjust layout to prevent overlap
plt.tight_layout()

# Save the figure to a file
plt.savefig(image_output_file)
# plt.show()

print(f"Chart saved as {image_output_file}")
