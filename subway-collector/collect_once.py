"""GitHub Actions cron에서 1회 수집 후 종료하는 엔트리포인트."""
import logging
from dotenv import load_dotenv

from collector.subway_api import fetch_all_stations_arrivals, MAJOR_STATIONS
from collector.kafka_producer import SubwayKafkaProducer
from collector.event_publisher import EventPublisher

load_dotenv()

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s - %(message)s",
)
logger = logging.getLogger(__name__)


def main():
    producer = SubwayKafkaProducer()
    event_publisher = EventPublisher(producer)
    try:
        arrivals = fetch_all_stations_arrivals(MAJOR_STATIONS)
        if arrivals:
            producer.publish_arrivals(arrivals)
            event_publisher.publish_all(arrivals)
            logger.info("수집 완료: %d건 발행", len(arrivals))
        else:
            logger.warning("수집 결과 없음 (API 한도 초과 또는 운행 없음)")
    finally:
        producer.close()


if __name__ == "__main__":
    main()
