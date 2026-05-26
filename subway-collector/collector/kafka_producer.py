import json
import logging
import os
import time
from kafka import KafkaProducer
from kafka.errors import NoBrokersAvailable

logger = logging.getLogger(__name__)

KAFKA_BOOTSTRAP_SERVERS = os.getenv("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092")
TOPIC_SUBWAY_REALTIME = "subway-realtime"

MAX_RETRIES = 10
RETRY_DELAY_SEC = 5


def create_producer() -> KafkaProducer:
    """Kafka 브로커 연결 재시도 포함 프로듀서 생성"""
    for attempt in range(1, MAX_RETRIES + 1):
        try:
            producer = KafkaProducer(
                bootstrap_servers=KAFKA_BOOTSTRAP_SERVERS,
                value_serializer=lambda v: json.dumps(v, ensure_ascii=False).encode("utf-8"),
                key_serializer=lambda k: k.encode("utf-8") if k else None,
                retries=3,
                acks="all",
            )
            logger.info("Kafka 연결 성공 (%s)", KAFKA_BOOTSTRAP_SERVERS)
            return producer
        except NoBrokersAvailable:
            logger.warning("Kafka 연결 실패 (시도 %d/%d), %d초 후 재시도...", attempt, MAX_RETRIES, RETRY_DELAY_SEC)
            if attempt < MAX_RETRIES:
                time.sleep(RETRY_DELAY_SEC)
    raise RuntimeError(f"Kafka 연결 실패: {KAFKA_BOOTSTRAP_SERVERS}")


class SubwayKafkaProducer:
    def __init__(self):
        self._producer = create_producer()

    def publish_arrivals(self, arrivals: list[dict]) -> None:
        """지하철 도착정보를 subway-realtime 토픽에 발행"""
        for arrival in arrivals:
            try:
                key = arrival.get("station_name", "unknown")
                self._producer.send(TOPIC_SUBWAY_REALTIME, key=key, value=arrival)
            except Exception as e:
                logger.error("Kafka 발행 실패 (station=%s): %s", arrival.get("station_name"), e)

        try:
            self._producer.flush(timeout=10)
            logger.debug("Kafka flush 완료 (%d건)", len(arrivals))
        except Exception as e:
            logger.error("Kafka flush 실패: %s", e)

    def close(self) -> None:
        self._producer.close()
