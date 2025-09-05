#!/usr/bin/env python3
import json
import requests
import sys
import time
from tabulate import tabulate

# Configuration
API_URL = "http://localhost:8089/payments/decide"  # Update with your actual API endpoint
API_KEY = "payment-api-key"  # Update with your actual API key

def load_test_cases(file_path):
    """Load test cases from JSON file"""
    with open(file_path, 'r') as f:
        return json.load(f)

def evaluate_test_cases(test_cases):
    """Evaluate each test case against the payment API"""
    results = []
    correct = 0
    total = len(test_cases)

    headers = {
        "Content-Type": "application/json",
        "X-API-Key": API_KEY
    }

    for case in test_cases:
        case_id = case["id"]
        description = case["description"]
        expected = case["expectedDecision"].upper()  # Convert expected to uppercase

        try:
            # Add small delay to avoid overwhelming the service
            time.sleep(0.5)

            # Send request to payment API
            response = requests.post(API_URL, json=case["request"], headers=headers)

            if response.status_code == 200:
                # Convert actual decision to uppercase for case-insensitive comparison
                actual_decision = response.json().get("decision", "").upper()
                is_correct = actual_decision == expected

                if is_correct:
                    correct += 1

                results.append([
                    case_id,
                    description,
                    expected,
                    response.json().get("decision", "").upper(),  # Keep original case for display
                    "✓" if is_correct else "✗",
                    ", ".join(response.json().get("reasons", []))
                ])
            else:
                results.append([
                    case_id,
                    description,
                    expected,
                    f"ERROR: {response.status_code}",
                    "✗",
                    response.text[:500] + "..." if len(response.text) > 50 else response.text
                ])
        except Exception as e:
            results.append([
                case_id,
                description,
                expected,
                f"ERROR: {str(e)}",
                "✗",
                ""
            ])

    accuracy = (correct / total) * 100 if total > 0 else 0
    return results, accuracy

def main():
    """Main function to run the evaluation"""
    if len(sys.argv) < 2:
        print("Usage: python evaluate_decisions.py <test_cases_file.json>")
        sys.exit(1)

    test_cases_file = sys.argv[1]
    test_cases = load_test_cases(test_cases_file)

    print(f"Evaluating {len(test_cases)} test cases against {API_URL}...")
    results, accuracy = evaluate_test_cases(test_cases)

    # Print results table
    headers = ["Test ID", "Description", "Expected", "Actual", "Correct", "Reasons"]
    print(tabulate(results, headers=headers, tablefmt="grid"))

    # Print summary
    print(f"\nAccuracy: {accuracy:.2f}% ({sum(1 for r in results if r[4] == '✓')}/{len(results)})")

    # Return non-zero exit code if accuracy is not 100%
    if accuracy < 100:
        sys.exit(1)

if __name__ == "__main__":
    main()