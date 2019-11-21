package baseentities.info;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import common.Common;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

public class GetMultipleStreams extends Common {

	static String apiPath;
	String api;
	static String[] params;
	Response apiResponse = null;
	String apiResponseMsgExpected;
	static String testDataFilePath;
	int streamId;

	static ArrayList<Integer> responseTimes = new ArrayList<Integer>();

	@BeforeClass
	public void doItBeforeTest() {
		loadPropertiesFromConfig("baseentities");
		// api path
		apiPath = "baseentities/api/v1/info/getMultipleStreams";
		api = serverPath + apiPath;
		// API params in sequence followed by apiResponseMsgExpected
		params = new String[] { "streamIds", "apiResponseMsgExpected" };
		// test data file
		testDataFilePath = "//src//test//resources//baseentities//info//GetMultipleStreams.xlsx";

	}

	@DataProvider
	public static String[][] testData() throws Exception {
		// The number of times data is repeated, test will be executed the same no. of
		// times{{1,1},{2,2}}
		String[][] testData = prepareTestData(params, testDataFilePath);
		return testData;
	}

	@Test(dataProvider = "testData", priority = 1)
	public void verifyGetMultipleStreamsApi(String streamIds, String apiResponseMsgExpected) {
		this.apiResponseMsgExpected = apiResponseMsgExpected;

		// pass api params and hit api
		if (streamIds.equals("ignoreHeader"))
			apiResponse = RestAssured.given().when().post(api).then().extract().response();
		else if (streamIds.equals("ignoreInput"))
			apiResponse = RestAssured.given().header("AUTHREQUEST", "INFOEDGE_SHIKSHA").when().post(api).then()
					.extract().response();
		else if (!streamIds.equals("ignoreInput"))
			apiResponse = RestAssured.given().header("AUTHREQUEST", "INFOEDGE_SHIKSHA").param("streamIds[]", streamIds)
					.when().post(api).then().extract().response();

		// fetch status code from response
		int statusCode = apiResponse.getStatusCode();
		JsonPath jsonPath = apiResponse.jsonPath();

		if (statusCode == 200) { // Positive case
			Reporter.log("Positive case", true);
			verifyPostiveCases(streamIds, jsonPath);
		} else if (statusCode == 400) { // Negative case
			Reporter.log("Negative case", true);
			verifyNegativeCases(params, jsonPath, null, apiResponseMsgExpected);
		} else if (statusCode == 403) { // unauthorized request
			Reporter.log("Unauthorized request case", true);
			verifyUnauthorizedRequestCase(jsonPath);
		} else {
			Reporter.log(", InCorrect Response Code : " + statusCode, true);
			Assert.fail("InCorrect Response Code : " + statusCode);
		}

		long apiResponseActualTime = apiResponse.getTimeIn(TimeUnit.MILLISECONDS);
		responseTimes.add((int) apiResponseActualTime);

		if (apiResponseActualTime > apiResponseExpectedTime) {
			Reporter.log("<br> API Response time : " + apiResponseActualTime + " MS is greater than expected time ("
					+ apiResponseExpectedTime + " MS)", true);
			Assert.fail("API Response time : " + apiResponseActualTime + " MS is greater than expected time ("
					+ apiResponseExpectedTime + " MS)");
		} else {
			Reporter.log("<br> API Response time : " + apiResponseActualTime + " MS", true);
		}
	}

	public void verifyPostiveCases(String streamIds, JsonPath jsonPath) {
		try {
			// verify status is success
			String statusResponse = jsonPath.getString("status");
			if (statusResponse.contentEquals("success")) {
				Reporter.log(", Correct status [success]", true);
			} else {
				Reporter.log(", InCorrect status [" + statusResponse + "]", true);
				Assert.fail("InCorrect status [" + statusResponse + "]");
			}

			// verify message is null
			String messageResponse = jsonPath.getString("message");
			if (messageResponse == null) {
				Reporter.log(", Correct message [null]", true);
			} else {
				Reporter.log(", InCorrect message [" + messageResponse + "]", true);
				Assert.fail("InCorrect message [" + messageResponse + "]");
			}

			// loop for multiple streamIds in array
			String[] streamIdsArr = streamIds.split(",");
			for (int i = 0; i < streamIdsArr.length; i++) {
				this.streamId = Integer.parseInt(streamIdsArr[i]);

				// fetch data of streamId from api response [jsonPath]
				String dataOfSingleStreamInResponse = jsonPath.getString("data." + streamId);

				// fetch streamName from streams
				ResultSet rs = exceuteDbQuery(
						"SELECT name, alias, display_order, synonym FROM streams where status = 'live' and stream_id = " + streamId + ";",
						"shiksha");

				// get row count of resultSet of sql query
				int rowcount = getCountOfResultSet(rs);

				if (rowcount > 0) {

					rs.next();

					// verify streamId in data[response] as same in db
					int streamIdOfSinglestreamInDataResponse = jsonPath.getInt("data." + streamId + ".streamId");
					if (streamIdOfSinglestreamInDataResponse == streamId) {
						Reporter.log(", Correct value of streamId [" + streamIdOfSinglestreamInDataResponse
								+ "] for streamId : " + streamId + "", true);
					} else {
						Reporter.log(", InCorrect value of streamId for streamId: " + streamId + " ["
								+ streamIdOfSinglestreamInDataResponse + "]", true);
						Assert.fail("InCorrect value of streamId for streamId : " + streamId + " ["
								+ streamIdOfSinglestreamInDataResponse + "]");
					}

					// verify name in data[response] as same in db
					String streamNameDb = rs.getString("name");
					String nameOfSingleStreamInDataResponse = jsonPath.getString("data." + streamId + ".name");
					if (nameOfSingleStreamInDataResponse.equalsIgnoreCase(streamNameDb)) {
						Reporter.log(", Correct name [" + nameOfSingleStreamInDataResponse + "] of streamId : "
								+ streamId + "", true);
					} else {
						Reporter.log(", InCorrect name of streamId: " + streamId + " ["
								+ nameOfSingleStreamInDataResponse + "]", true);
						Assert.fail("InCorrect name of streamId : " + streamId + " [" + nameOfSingleStreamInDataResponse
								+ "]");
					}

					// verify alias in data[response] as same in db
					String aliasDb = rs.getString("alias");
					if (aliasDb.equals(""))
						aliasDb = null;
					String aliasOfSinglestreamInDataResponse = jsonPath.getString("data." + streamId + ".alias");
					if (aliasOfSinglestreamInDataResponse == aliasDb) {
						Reporter.log(", Correct alias [" + aliasOfSinglestreamInDataResponse + "] of streamId : "
								+ streamId + "", true);
					} else {
						Reporter.log(", InCorrect alias of streamId: " + streamId + " ["
								+ aliasOfSinglestreamInDataResponse + "]", true);
						Assert.fail("InCorrect alias of streamId : " + streamId + " ["
								+ aliasOfSinglestreamInDataResponse + "]");
					}

					// verify display_order in data[response] as same in db
					String displayOrderDb = rs.getString("display_order");
					String displayOrderOfSinglestreamInDataResponse = jsonPath
							.getString("data." + streamId + ".displayOrder");
					if (displayOrderOfSinglestreamInDataResponse.equalsIgnoreCase(displayOrderDb)) {
						Reporter.log(", Correct displayOrder [" + displayOrderOfSinglestreamInDataResponse
								+ "] of streamId : " + streamId + "", true);
					} else {
						Reporter.log(", InCorrect displayOrder of streamId: " + streamId + " ["
								+ displayOrderOfSinglestreamInDataResponse + "]", true);
						Assert.fail("InCorrect displayOrder of streamId : " + streamId + " ["
								+ displayOrderOfSinglestreamInDataResponse + "]");
					}
					
					// verify synonyms in data[response] as same in db
					String synonymsDb = "["+rs.getString("synonym")+"]";
					String synonymsOfSinglestreamInDataResponse = jsonPath
							.getString("data." + streamId + ".synonyms");
					if (synonymsOfSinglestreamInDataResponse.equalsIgnoreCase(synonymsDb)) {
						Reporter.log(", Correct synonyms [" + synonymsOfSinglestreamInDataResponse
								+ "] of streamId : " + streamId + "", true);
					} else {
						Reporter.log(", InCorrect synonyms of streamId: " + streamId + " ["
								+ synonymsOfSinglestreamInDataResponse + "]", true);
						Assert.fail("InCorrect synonyms of streamId : " + streamId + " ["
								+ synonymsOfSinglestreamInDataResponse + "]");
					}

				} else {
					// if streamId doesn't exist in db, then no data would be returned for that
					// particular streamId
					if (dataOfSingleStreamInResponse == null) {
						Reporter.log(", No data returned for streamId : " + streamId + " [Correct]", true);
					} else {
						Reporter.log(", InCorrect data of streamId : " + streamId + " ["
								+ dataOfSingleStreamInResponse + "]", true);
						Assert.fail("InCorrect data of streamId : " + streamId + " [" + dataOfSingleStreamInResponse
								+ "]");
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
			Assert.fail("SQLException occured :" + e.getMessage());
		}
	}

	@Test(priority = 2)
	public static void respnseTimeStats_GetMultipleStreams() {
		respnseTimeStatsCommon(responseTimes, apiPath.substring(apiPath.lastIndexOf("/") + 1));
	}
}