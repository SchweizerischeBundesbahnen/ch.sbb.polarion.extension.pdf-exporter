<%@ page contentType="text/html; charset=UTF-8" %>
<jsp:include page="/common/jsp/about.jsp"/>

<style>
    .weasyprint-widget {
        margin: 20px 0;
        padding: 15px;
        border: 1px solid #ddd;
        border-radius: 4px;
        background-color: #f9f9f9;
        max-width: fit-content;
    }
    .weasyprint-widget h3 {
        margin: 0 0 15px 0;
        padding-bottom: 10px;
        border-bottom: 2px solid #003366;
        color: #003366;
    }
    .wp-status-row {
        display: flex;
        align-items: center;
        gap: 10px;
        margin-bottom: 10px;
    }
    .wp-status-indicator {
        width: 12px;
        height: 12px;
        border-radius: 50%;
        background-color: #ccc;
    }
    .wp-status-indicator.healthy {
        background-color: #4caf50;
    }
    .wp-status-indicator.unhealthy {
        background-color: #f44336;
    }
    .wp-status-indicator.warning {
        background-color: #ff9800;
    }
    .wp-metrics-grid {
        display: grid;
        grid-template-columns: repeat(4, minmax(160px, auto));
        gap: 10px;
        margin-top: 10px;
        justify-content: start;
    }
    .wp-metric-card {
        padding: 10px;
        background-color: white;
        border: 1px solid #e0e0e0;
        border-radius: 3px;
    }
    .wp-metric-label {
        font-size: 11px;
        color: #666;
        text-transform: uppercase;
    }
    .wp-metric-value {
        font-size: 18px;
        font-weight: bold;
        color: #003366;
        margin-top: 3px;
    }
    .wp-metric-value.error {
        color: #f44336;
    }
    .wp-metric-value.warning {
        color: #ff9800;
    }
    .wp-error {
        padding: 10px;
        background-color: #fff3cd;
        border: 1px solid #ffc107;
        border-radius: 3px;
        color: #856404;
    }
    .wp-loading {
        padding: 10px;
        color: #666;
        font-style: italic;
    }
    .wp-refresh-info {
        font-size: 11px;
        color: #999;
        margin-top: 10px;
    }
    .wp-header-row {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 15px;
    }
    .wp-header-row h3 {
        margin: 0;
    }
</style>

<script>
    (function () {
        // Store interval ID for cleanup
        let healthCheckIntervalId = null;

        // override help and disclaimer links behavior - jump to the menu item instead of external MD file page
        document.addEventListener('DOMContentLoaded', function () {
            document.querySelectorAll('a').forEach(function (element) {
                if (element.innerHTML === 'user guide') {
                    replaceAnchorUrl(element, '/user-guide');
                }
                if (element.innerHTML === 'disclaimer') {
                    replaceAnchorUrl(element, '/disclaimer');
                }
            });

            // Initialize WeasyPrint widget - insert before markdown article
            initWeasyPrintWidget();
        });

        // Clean up interval on page unload to prevent memory leaks
        window.addEventListener('beforeunload', function () {
            if (healthCheckIntervalId !== null) {
                clearInterval(healthCheckIntervalId);
                healthCheckIntervalId = null;
            }
        });

        function replaceAnchorUrl(anchor, targetPath) {
            const url = top.location.href.replace('/about', targetPath);
            anchor.href = url;
            anchor.target = "";
            anchor.onclick = function(event) {
                event.preventDefault();
                parent.document.location = url;
            };
        }

        function initWeasyPrintWidget() {
            // Find the markdown article element
            const article = document.querySelector('article.markdown-body');
            if (!article) {
                console.warn('WeasyPrint widget: article.markdown-body not found');
                return;
            }

            // Create widget container
            const widget = document.createElement('div');
            widget.id = 'weasyprintWidget';
            widget.innerHTML = '<div class="weasyprint-widget"><h3>WeasyPrint Service Status</h3><div class="wp-loading">Loading service status...</div></div>';

            // Insert widget before the article
            article.parentNode.insertBefore(widget, article);

            // Start monitoring
            loadWeasyPrintHealth();
            // Refresh every 5 seconds
            healthCheckIntervalId = setInterval(loadWeasyPrintHealth, 5000);
        }

        function loadWeasyPrintHealth() {
            const widget = document.getElementById('weasyprintWidget');
            if (!widget) return;

            fetch('/polarion/pdf-exporter/rest/internal/configuration/weasyprint-health')
                .then(function(response) {
                    if (response.ok) {
                        return response.json();
                    }
                    throw new Error('Service unavailable');
                })
                .then(function(data) {
                    renderHealthWidget(data);
                })
                .catch(function(error) {
                    widget.innerHTML = '<div class="weasyprint-widget">' +
                        '<div class="wp-header-row">' +
                        '<h3>WeasyPrint Service Status</h3>' +
                        '</div>' +
                        '<div class="wp-status-row">' +
                        '<span class="wp-status-indicator unhealthy"></span>' +
                        '<strong>Service Unavailable</strong>' +
                        '</div>' +
                        '<div class="wp-error">Unable to connect to WeasyPrint service</div>' +
                        '</div>';
                });
        }

        function renderHealthWidget(data) {
            const widget = document.getElementById('weasyprintWidget');
            const metrics = data.metrics || {};

            const statusClass = data.status === 'healthy' ? 'healthy' :
                               (data.chromium_running === false ? 'unhealthy' : 'warning');

            const uptimeHours = metrics.uptime_seconds ? (metrics.uptime_seconds / 3600).toFixed(1) : '0';

            const pdfErrorRate = metrics.error_pdf_generation_rate_percent || 0;
            const pdfErrorClass = pdfErrorRate > 5 ? 'error' : (pdfErrorRate > 1 ? 'warning' : '');

            const svgErrorRate = metrics.error_svg_conversion_rate_percent || 0;
            const svgErrorClass = svgErrorRate > 5 ? 'error' : (svgErrorRate > 1 ? 'warning' : '');

            const html = '<div class="weasyprint-widget">' +
                '<div class="wp-header-row">' +
                '<h3>WeasyPrint Service Status</h3>' +
                '</div>' +

                '<div class="wp-metrics-grid">' +

                // Status
                '<div class="wp-metric-card">' +
                '<div class="wp-metric-label">Status</div>' +
                '<div class="wp-metric-value">' +
                '<span class="wp-status-indicator ' + statusClass + '" style="display: inline-block; vertical-align: middle;"></span> ' +
                '<span style="vertical-align: middle;">' + (data.status || 'unknown').toUpperCase() + '</span>' +
                '</div>' +
                '</div>' +

                // Uptime
                '<div class="wp-metric-card">' +
                '<div class="wp-metric-label">Uptime</div>' +
                '<div class="wp-metric-value">' + uptimeHours + ' h</div>' +
                '</div>' +

                // Concurrent Slots
                '<div class="wp-metric-card">' +
                '<div class="wp-metric-label">Concurrent Slots</div>' +
                '<div class="wp-metric-value">' + (metrics.max_concurrent_pdf_generations || 0) + '</div>' +
                '</div>' +

                // Queue
                '<div class="wp-metric-card">' +
                '<div class="wp-metric-label">Queue</div>' +
                '<div class="wp-metric-value">' + (metrics.queue_size || 0) + '</div>' +
                '</div>' +

                // PDF Generations
                '<div class="wp-metric-card">' +
                '<div class="wp-metric-label">PDF Generations</div>' +
                '<div class="wp-metric-value">' + (metrics.pdf_generations || 0).toLocaleString() + '</div>' +
                '</div>' +

                // Avg Generation Time
                '<div class="wp-metric-card">' +
                '<div class="wp-metric-label">Avg Generation Time</div>' +
                '<div class="wp-metric-value">' + (metrics.avg_pdf_generation_time_ms || 0).toFixed(0) + ' ms</div>' +
                '</div>' +

                // Failed Generations
                '<div class="wp-metric-card">' +
                '<div class="wp-metric-label">Failed Generations</div>' +
                '<div class="wp-metric-value">' + (metrics.failed_pdf_generations || 0) + '</div>' +
                '</div>' +

                // Generation Error Rate
                '<div class="wp-metric-card">' +
                '<div class="wp-metric-label">Generation Error Rate</div>' +
                '<div class="wp-metric-value ' + pdfErrorClass + '">' + pdfErrorRate.toFixed(2) + '%</div>' +
                '</div>' +

                // SVG Conversions
                '<div class="wp-metric-card">' +
                '<div class="wp-metric-label">SVG Conversions</div>' +
                '<div class="wp-metric-value">' + (metrics.total_svg_conversions || 0).toLocaleString() + '</div>' +
                '</div>' +

                // Avg Conversion Time
                '<div class="wp-metric-card">' +
                '<div class="wp-metric-label">Avg Conversion Time</div>' +
                '<div class="wp-metric-value">' + (metrics.avg_svg_conversion_time_ms || 0).toFixed(0) + ' ms</div>' +
                '</div>' +

                // Failed Conversions
                '<div class="wp-metric-card">' +
                '<div class="wp-metric-label">Failed Conversions</div>' +
                '<div class="wp-metric-value">' + (metrics.failed_svg_conversions || 0) + '</div>' +
                '</div>' +

                // Conversion Error Rate
                '<div class="wp-metric-card">' +
                '<div class="wp-metric-label">Conversion Error Rate</div>' +
                '<div class="wp-metric-value ' + svgErrorClass + '">' + svgErrorRate.toFixed(2) + '%</div>' +
                '</div>' +

                '</div>' +

                '<div class="wp-refresh-info">Auto-refreshes every 5 seconds • Last check: ' + (metrics.last_health_check || 'N/A') + '</div>' +
                '</div>';

            widget.innerHTML = html;
        }
    })();
</script>
