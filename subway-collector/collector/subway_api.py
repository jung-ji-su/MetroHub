import os
import requests
import logging
from dotenv import load_dotenv

load_dotenv()

logger = logging.getLogger(__name__)

BASE_URL = "http://swopenAPI.seoul.go.kr/api/subway"
API_KEY = os.getenv("SEOUL_API_KEY", "")


def fetch_realtime_arrivals(station_name: str) -> list[dict]:
    """서울 열린데이터광장 지하철 실시간 도착정보 조회"""
    url = f"{BASE_URL}/{API_KEY}/json/realtimeStationArrival/0/50/{station_name}"
    try:
        response = requests.get(url, timeout=10)
        response.raise_for_status()
        return parse_arrivals(response.json())
    except requests.exceptions.RequestException as e:
        logger.error("API 호출 실패 (station=%s): %s", station_name, e)
        return []


def parse_arrivals(raw: dict) -> list[dict]:
    """API 응답에서 열차 도착 정보 파싱"""
    try:
        result_code = raw.get("errorMessage", {}).get("status", 0)
        if result_code != 200:
            logger.warning("API 오류 응답: %s", raw.get("errorMessage"))
            return []

        arrivals = raw.get("realtimeArrivalList", [])
        return [
            {
                "station_name": item.get("statnNm"),
                "line_number": item.get("subwayId"),
                "train_no": item.get("btrainNo"),
                "arrival_message": item.get("arvlMsg2"),
                "direction": item.get("trainLineNm"),
                "congestion_level": item.get("congestionTrain"),
                "updated_at": item.get("recptnDt"),
            }
            for item in arrivals
        ]
    except (KeyError, TypeError) as e:
        logger.error("응답 파싱 실패: %s", e)
        return []


def fetch_all_stations_arrivals(station_names: list[str]) -> list[dict]:
    """여러 역의 실시간 도착정보 일괄 조회"""
    results = []
    for station in station_names:
        arrivals = fetch_realtime_arrivals(station)
        results.extend(arrivals)
    return results


MAJOR_STATIONS = [
    "서울역", "강남", "홍대입구", "신촌", "건대입구",
    "잠실", "신림", "수원", "인천", "사당",
]
