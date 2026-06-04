"""GitHub Actions cron에서 1회 수집 후 종료하는 엔트리포인트."""
import logging
from concurrent.futures import ThreadPoolExecutor, as_completed
from dotenv import load_dotenv

from collector.subway_api import fetch_realtime_arrivals, ALL_STATIONS
from collector.kafka_producer import SubwayKafkaProducer
from collector.event_publisher import EventPublisher

load_dotenv()

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s - %(message)s",
)
logger = logging.getLogger(__name__)

_MAX_WORKERS = 30


def main():
    producer = SubwayKafkaProducer()
    event_publisher = EventPublisher(producer)
    try:
        logger.info("수집 시작 (전 역 %d개 병렬 조회)", len(ALL_STATIONS))
        arrivals = []
        with ThreadPoolExecutor(max_workers=_MAX_WORKERS) as executor:
            futures = {executor.submit(fetch_realtime_arrivals, s): s for s in ALL_STATIONS}
            for future in as_completed(futures):
                try:
                    arrivals.extend(future.result())
                except Exception as e:
                    logger.error("수집 실패 (station=%s): %s", futures[future], e)

        if arrivals:
            producer.publish_arrivals(arrivals)
            event_publisher.publish_all(arrivals)
            logger.info("수집 완료: %d건 발행", len(arrivals))
        else:
            logger.warning("수집 결과 없음 (운행 없음 또는 API 오류)")
    finally:
        producer.close()


if __name__ == "__main__":
    main()
