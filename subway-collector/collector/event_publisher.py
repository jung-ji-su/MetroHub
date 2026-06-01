import logging
from collections import defaultdict

logger = logging.getLogger(__name__)

TOPIC_CONGESTION_UPDATED = "subway.congestion.updated"
TOPIC_LINE_ALERT = "subway.line.alert"
TOPIC_TRENDING_UPDATED = "subway.trending.updated"

SPIKE_THRESHOLD = 20  # 이전 대비 혼잡도 급등 기준


class EventPublisher:
    def __init__(self, producer):
        self._producer = producer
        self._prev_line_congestion: dict[str, float] = {}

    def publish_all(self, arrivals: list[dict]) -> None:
        if not arrivals:
            return
        self._publish_congestion_update(arrivals)
        self._detect_and_alert(arrivals)
        self._publish_trending(arrivals)

    # ── subway.congestion.updated ─────────────────────────────────────
    def _publish_congestion_update(self, arrivals: list[dict]) -> None:
        line_levels: dict[str, list[int]] = defaultdict(list)
        for a in arrivals:
            lvl = a.get("congestion_level")
            if lvl is not None:
                try:
                    line_levels[a["line_number"]].append(int(lvl))
                except (ValueError, TypeError):
                    pass

        for line, levels in line_levels.items():
            avg = round(sum(levels) / len(levels), 1)
            self._producer.publish_event(
                TOPIC_CONGESTION_UPDATED,
                key=line,
                value={"lineNumber": line, "avgCongestion": avg, "sampleCount": len(levels)},
            )

    # ── subway.line.alert ────────────────────────────────────────────
    def _detect_and_alert(self, arrivals: list[dict]) -> None:
        line_levels: dict[str, list[int]] = defaultdict(list)
        for a in arrivals:
            lvl = a.get("congestion_level")
            if lvl is not None:
                try:
                    line_levels[a["line_number"]].append(int(lvl))
                except (ValueError, TypeError):
                    pass

        for line, levels in line_levels.items():
            avg = sum(levels) / len(levels)
            prev = self._prev_line_congestion.get(line)

            if prev is not None and (avg - prev) >= SPIKE_THRESHOLD:
                self._producer.publish_event(
                    TOPIC_LINE_ALERT,
                    key=line,
                    value={
                        "lineNumber": line,
                        "alertType": "CONGESTION_SPIKE",
                        "message": f"혼잡도 급등 감지 ({prev:.0f} → {avg:.0f})",
                        "severity": "HIGH" if avg > 80 else "MEDIUM",
                    },
                )
                logger.info("혼잡도 급등: 노선 %s (%.0f → %.0f)", line, prev, avg)

            self._prev_line_congestion[line] = avg

    # ── subway.trending.updated ──────────────────────────────────────
    def _publish_trending(self, arrivals: list[dict]) -> None:
        # 혼잡도 최대값 or 도착 열차 수로 역 점수 산정
        station_score: dict[str, int] = defaultdict(int)
        for a in arrivals:
            station = a.get("station_name")
            if not station:
                continue
            lvl = a.get("congestion_level")
            if lvl is not None:
                try:
                    station_score[station] = max(station_score[station], int(lvl))
                except (ValueError, TypeError):
                    station_score[station] += 10
            else:
                station_score[station] += 10  # 열차 도착 수로 카운트

        top5 = sorted(station_score.items(), key=lambda x: x[1], reverse=True)[:5]
        if top5:
            self._producer.publish_event(
                TOPIC_TRENDING_UPDATED,
                key="trending",
                value={
                    "stations": [
                        {"stationName": name, "score": score}
                        for name, score in top5
                    ]
                },
            )
