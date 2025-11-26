import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

const errorRate = new Rate('errors');
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export const options = {
    stages: [
        { duration: '30s', target: 10 },  // Ramp-up to 10 users
        { duration: '1m', target: 50 },   // Ramp-up to 50 users
        { duration: '2m', target: 50 },   // Stay at 50 users
        { duration: '30s', target: 0 },   // Ramp-down to 0 users
    ],
    thresholds: {
        http_req_duration: ['p(95)<500', 'p(99)<1000'], // 95% < 500ms, 99% < 1000ms
        errors: ['rate<0.1'],  // Error rate should be less than 10%
        http_req_failed: ['rate<0.05'], // Failed requests less than 5%
    },
};

function generateUniqueDealId() {
    return `DEAL-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
}

function generateRandomDeal() {
    const currencies = ['USD', 'EUR', 'GBP', 'JPY', 'CHF', 'CAD', 'AUD'];
    const fromCurrency = currencies[Math.floor(Math.random() * currencies.length)];
    let toCurrency = currencies[Math.floor(Math.random() * currencies.length)];

    // Ensure from and to currencies are different
    while (toCurrency === fromCurrency) {
        toCurrency = currencies[Math.floor(Math.random() * currencies.length)];
    }

    const timestamp = new Date(Date.now() - Math.random() * 86400000).toISOString();
    const amount = (Math.random() * 100000 + 1000).toFixed(4);

    return {
        dealUniqueId: generateUniqueDealId(),
        fromCurrencyIsoCode: fromCurrency,
        toCurrencyIsoCode: toCurrency,
        dealTimestamp: timestamp,
        dealAmount: parseFloat(amount)
    };
}

export default function () {
    // Test 1: Import single deal
    const deal = generateRandomDeal();
    const importResponse = http.post(
        `${BASE_URL}/api/v1/deals`,
        JSON.stringify(deal),
        {
            headers: { 'Content-Type': 'application/json' },
        }
    );

    const importSuccess = check(importResponse, {
        'import status is 201': (r) => r.status === 201,
        'import response has dealUniqueId': (r) => r.json('dealUniqueId') !== undefined,
        'import response time < 500ms': (r) => r.timings.duration < 500,
    });

    errorRate.add(!importSuccess);

    sleep(1);

    // Test 2: Try to import duplicate (should fail with 409)
    const duplicateResponse = http.post(
        `${BASE_URL}/api/v1/deals`,
        JSON.stringify(deal),
        {
            headers: { 'Content-Type': 'application/json' },
        }
    );

    check(duplicateResponse, {
        'duplicate returns 409': (r) => r.status === 409,
        'duplicate error message present': (r) => r.json('error') !== undefined,
    });

    sleep(1);

    // Test 3: Retrieve all deals
    const getAllResponse = http.get(`${BASE_URL}/api/v1/deals`);

    const getAllSuccess = check(getAllResponse, {
        'get all status is 200': (r) => r.status === 200,
        'get all returns array': (r) => Array.isArray(r.json()),
        'get all response time < 1000ms': (r) => r.timings.duration < 1000,
    });

    errorRate.add(!getAllSuccess);

    sleep(2);

    // Test 4: Bulk import (3 deals)
    const bulkDeals = [
        generateRandomDeal(),
        generateRandomDeal(),
        generateRandomDeal(),
    ];

    const bulkResponse = http.post(
        `${BASE_URL}/api/v1/deals/bulk`,
        JSON.stringify(bulkDeals),
        {
            headers: { 'Content-Type': 'application/json' },
        }
    );

    const bulkSuccess = check(bulkResponse, {
        'bulk import status is 201': (r) => r.status === 201,
        'bulk import returns array': (r) => Array.isArray(r.json()),
        'bulk import response time < 2000ms': (r) => r.timings.duration < 2000,
    });

    errorRate.add(!bulkSuccess);

    sleep(2);

    // Test 5: Health check
    const healthResponse = http.get(`${BASE_URL}/api/v1/deals/health`);

    check(healthResponse, {
        'health check status is 200': (r) => r.status === 200,
        'health check response time < 100ms': (r) => r.timings.duration < 100,
    });
}

export function handleSummary(data) {
    return {
        'summary.json': JSON.stringify(data),
        stdout: textSummary(data, { indent: ' ', enableColors: true }),
    };
}

function textSummary(data, options) {
    const indent = options.indent || '';
    const enableColors = options.enableColors || false;

    let output = '\n';
    output += `${indent}Test Summary:\n`;
    output += `${indent}============\n`;
    output += `${indent}Total Requests: ${data.metrics.http_reqs.values.count}\n`;
    output += `${indent}Failed Requests: ${data.metrics.http_req_failed.values.passes}\n`;
    output += `${indent}Request Duration (avg): ${data.metrics.http_req_duration.values.avg.toFixed(2)}ms\n`;
    output += `${indent}Request Duration (p95): ${data.metrics.http_req_duration.values['p(95)'].toFixed(2)}ms\n`;
    output += `${indent}Request Duration (p99): ${data.metrics.http_req_duration.values['p(99)'].toFixed(2)}ms\n`;

    return output;
}