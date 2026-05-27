import logging
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

if __name__ == "__main__":
    logger.info("subway-collector 시작: 초기 데이터 1회 수집")
    producer = SubwayKafkaProducer()
    try:
        arrivals = fetch_all_stations_arrivals(MAJOR_STATIONS)
        if arrivals:
            producer.publish_arrivals(arrivals)
            logger.info("초기 수집 완료: %d건 발행", len(arrivals))
        else:
            logger.warning("초기 수집 결과 없음 (API 한도 초과 또는 응답 없음)")
    except Exception as e:
        logger.error("초기 수집 실패: %s", e, exc_info=True)

    # on-demand 조회는 subway-api가 직접 처리
    # collector는 이후 대기 상태 유지 (재시작 방지)
    logger.info("subway-collector 대기 중. on-demand 조회는 subway-api가 처리합니다.")
    while True:
        time.sleep(86400)  # 24시간 대기 (재시작 방지용)
