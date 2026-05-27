import logging
import schedule
import time
from dotenv import load_dotenv

from collector.subway_api import fetch_all_stations_arrivals, MAJOR_STATIONS
from collector.kafka_producer import SubwayKafkaProducer

load_dotenv()

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s - %(message)s",
)
logger = logging.getLogger(__name__)

producer = SubwayKafkaProducer()


def collect_and_publish():
    logger.info("지하철 실시간 도착정보 수집 시작")
    arrivals = fetch_all_stations_arrivals(MAJOR_STATIONS)
    if arrivals:
        producer.publish_arrivals(arrivals)
        logger.info("수집 완료: %d건 발행", len(arrivals))
    else:
        logger.warning("수집 결과 없음")


if __name__ == "__main__":
    logger.info("subway-collector 시작")
    collect_and_publish()

    schedule.every(15).minutes.do(collect_and_publish)

    while True:
        try:
            schedule.run_pending()
        except Exception as e:
            logger.error("스케줄 실행 중 예외 발생: %s", e, exc_info=True)
        time.sleep(1)
