import logging
import schedule
import time
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

    def collect_cycle():
        logger.info("=== 수집 사이클 시작 ===")
        try:
            arrivals = fetch_all_stations_arrivals(MAJOR_STATIONS)
            if arrivals:
                producer.publish_arrivals(arrivals)       # subway-realtime (기존)
                event_publisher.publish_all(arrivals)     # 신규 이벤트 토픽들
                logger.info("수집 완료: %d건 발행", len(arrivals))
            else:
                logger.warning("수집 결과 없음 (API 한도 초과 또는 운행 없음)")
        except Exception as e:
            logger.error("수집 실패: %s", e, exc_info=True)

    logger.info("subway-collector 시작 — 30초 주기 수집")
    collect_cycle()  # 초기 1회 즉시 실행

    schedule.every(30).seconds.do(collect_cycle)
    while True:
        schedule.run_pending()
        time.sleep(1)


if __name__ == "__main__":
    main()
