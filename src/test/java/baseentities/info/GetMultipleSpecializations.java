package baseentities.info;

import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

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


public class GetMultipleSpecializations extends Common{
 
	static String apiPath;
	String api;
	static String[] params;
	Response apiResponse = null;
	String apiResponseMsgExpected;
	static String testDataFilePath;
	int specializationId;

	static ArrayList<Integer> responseTimes = new ArrayList<Integer>();

	@BeforeClass
	public void doItBeforeTest() {
		// apiResponseExpectedTime = 200; // in MS
		loadPropertiesFromConfig("baseentities");
		// api path
		apiPath = "baseentities/api/v1/info/getMultipleSpecializations ";
		api = serverPath + apiPath;
		// API params in sequence followed by apiResponseMsgExpected
		params = new String[] { "specializationIds", "apiResponseMsgExpected" };
		// test data file
		testDataFilePath = "//src//test//resources//baseentities//info//GetMultipleSpecializations.xlsx";

	}

	@DataProvider
	public static String[][] testData() throws Exception {
		// The number of times data is repeated, test will be executed the same no. of
		// times
		String[][] testData = prepareTestData(params, testDataFilePath);
		return testData;
	}

	@Test(dataProvider = "testData", priority = 1)
	public void verifyGetMultipleSpecializationApi(String SpecializationIds, String apiResponseMsgExpected) {
		this.apiResponseMsgExpected = apiResponseMsgExpected;

		// pass api params and hit api
		if (SpecializationIds.equals("ignoreHeader"))
			apiResponse = RestAssured.given().when().post(api).then().extract().response();
		else if (SpecializationIds.equals("ignoreInput"))
			apiResponse = RestAssured.given().header("AUTHREQUEST", "INFOEDGE_SHIKSHA").when().post(api).then()
					.extract().response();
		else if (!SpecializationIds.equals("ignoreInput"))
			apiResponse = RestAssured.given().header("AUTHREQUEST", "INFOEDGE_SHIKSHA")
					.param("specializationIds[]", SpecializationIds).when().post(api).then().extract().response();

		// fetch status code from response
		int statusCode = apiResponse.getStatusCode();
		JsonPath jsonPath = apiResponse.jsonPath();

		if (statusCode == 200) { // Positive case
			Reporter.log("Positive case", true);
			verifyPostiveCases(SpecializationIds, jsonPath);
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

	public void verifyPostiveCases(String primaryspecializationIds, JsonPath jsonPath) {
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

			// loop for multiple specilizationIds in array
			String[] primaryspecializationIdsArr = primaryspecializationIds.split(",");
			for (int i = 0; i < primaryspecializationIdsArr.length; i++) {
				this.specializationId = Integer.parseInt(primaryspecializationIdsArr[i]);

				// fetch data of specializationId from api response [jsonPath]
				String dataOfSingleSubStreamInResponse = jsonPath.getString("data." + specializationId);

				// fetch details from specializationId
				ResultSet rs = exceuteDbQuery(
						"SELECT specialization_id, name, primary_stream_id, primary_substream_id, alias, synonym, type FROM specializations where status = 'live' and specialization_id = "
								+ specializationId + ";",
						"shiksha");

				// get row count of resultSet of sql query
				int rowcount = getCountOfResultSet(rs);

				if (rowcount > 0) {

					rs.next();

					// verify specializationId in data[response] as same in db
					int SpecializationIdsOfSingleSpecializationInDataResponse = jsonPath
							.getInt("data." + specializationId + ".specializationId");
					if (SpecializationIdsOfSingleSpecializationInDataResponse == specializationId) {
						Reporter.log(", Correct value of specializationId [" + SpecializationIdsOfSingleSpecializationInDataResponse
								+ "] for specializationId : " + specializationId + "", true);
					} else {
						Reporter.log(", InCorrect value of specializationId for specializationId: " + specializationId + " ["
								+ SpecializationIdsOfSingleSpecializationInDataResponse + "]", true);
						Assert.fail("InCorrect value of specializationId for specializationId : " + specializationId + " ["
								+ SpecializationIdsOfSingleSpecializationInDataResponse + "]");
					}

					// verify name in data[response] as same in db
					String specializationNameDb = rs.getString("name");
					String nameOfSingleSpecializationInDataResponse = jsonPath.getString("data." + specializationId + ".name");
					if (nameOfSingleSpecializationInDataResponse.equalsIgnoreCase(specializationNameDb)) {
						Reporter.log(", Correct name [" + nameOfSingleSpecializationInDataResponse + "] of specializationId : "
								+ specializationId + "", true);
					} else {
						Reporter.log(", InCorrect name of specializationNameDb: " + specializationId + " ["
								+ nameOfSingleSpecializationInDataResponse + "]", true);
						Assert.fail("InCorrect name of specializationId : " + specializationId + " ["
								+ nameOfSingleSpecializationInDataResponse + "]");
					}

					// verify alias in data[response] as same in db
					String aliasDb = rs.getString("alias");
					if (aliasDb.equals(""))
						aliasDb = null;
					String aliasOfSingleSpecializationInDataResponse = jsonPath.getString("data." + specializationId + ".alias");
					if ((aliasDb == null && aliasOfSingleSpecializationInDataResponse == aliasDb) || (aliasOfSingleSpecializationInDataResponse.equalsIgnoreCase(aliasDb))) {
						Reporter.log(", Correct alias [" + aliasOfSingleSpecializationInDataResponse + "] of specializationId : "
								+ specializationId + "", true);
					} else {
						Reporter.log(", InCorrect alias of specializationId: " + specializationId + " ["
								+ aliasOfSingleSpecializationInDataResponse + "]", true);
						Assert.fail("InCorrect alias of specializationId : " + specializationId + " ["
								+ aliasOfSingleSpecializationInDataResponse + "]");
					}

					// verify synonyms in data[response] as same in db
					String synonymsDb = "[" + rs.getString("synonym") + "]";
					String synonymsOfSingleSpecializationInDataResponse = jsonPath
							.getString("data." + specializationId + ".synonym");
					if (synonymsOfSingleSpecializationInDataResponse.equalsIgnoreCase(synonymsDb)) {
						Reporter.log(", Correct synonyms [" + synonymsOfSingleSpecializationInDataResponse
								+ "] of specializationId : " + specializationId + "", true);
					} else {
						Reporter.log(", InCorrect synonyms of specializationId: " + specializationId + " ["
								+ synonymsOfSingleSpecializationInDataResponse + "]", true);
						Assert.fail("InCorrect synonyms of specializationId : " + specializationId + " ["
								+ synonymsOfSingleSpecializationInDataResponse + "]");
					}

					// verify primaryStreamId in data[response] as same in db
					String primaryStreamIdDb = rs.getString("primary_stream_id");
					String primaryStreamIdOfSinglespecializationInDataResponse = jsonPath
							.getString("data." + specializationId + ".primaryStreamId");
					if (primaryStreamIdOfSinglespecializationInDataResponse.equalsIgnoreCase(primaryStreamIdDb)) {
						Reporter.log(", Correct primaryStreamId [" + primaryStreamIdOfSinglespecializationInDataResponse
								+ "] of specializationId : " + specializationId + "", true);
					} else {
						Reporter.log("InCorrect primaryStreamId of specializationId: " + specializationId + " ["
								+ primaryStreamIdOfSinglespecializationInDataResponse + "]", true);
						Assert.fail("InCorrect primaryStreamId of specializationId : " + specializationId + " ["
								+ primaryStreamIdOfSinglespecializationInDataResponse + "]");
					}

					//verify primarysubstreamid in data[response] as same in db
					String primarySubStreamIdDb = rs.getString("primary_substream_id");
					String primarySubStreamIdOfSinglespecializationInDataResponse = jsonPath
							.getString("data." + specializationId + ".primarySubstreamId");
					if (primarySubStreamIdOfSinglespecializationInDataResponse.equalsIgnoreCase(primarySubStreamIdDb)) {
						Reporter.log(", Correct primarySubStreamId [" + primarySubStreamIdOfSinglespecializationInDataResponse
								+ "] of specializationId : " + specializationId + "", true);
					} else {
						Reporter.log("InCorrect primarySubStreamId of specializationId: " + specializationId + " ["
								+ primarySubStreamIdOfSinglespecializationInDataResponse + "]", true);
						Assert.fail("InCorrect primarySubStreamId of specializationId : " + specializationId + " ["
								+ primarySubStreamIdOfSinglespecializationInDataResponse + "]");
					}
					//verify type in data[response] as same in db
					String typeIdDb = rs.getString("type");
					String typeOfSinglespecializationInDataResponse = jsonPath
							.getString("data." + specializationId + ".type");
					if (typeOfSinglespecializationInDataResponse.equalsIgnoreCase(typeIdDb)) {
						Reporter.log(", Correct type [" + typeOfSinglespecializationInDataResponse
								+ "] of specializationId : " + specializationId + "", true);
					} else {
						Reporter.log("InCorrect primarySubStreamId of specializationId: " + specializationId + " ["
								+ typeOfSinglespecializationInDataResponse + "]", true);
						Assert.fail("InCorrect primarySubStreamId of specializationId : " + specializationId + " ["
								+ typeOfSinglespecializationInDataResponse + "]");
					}
					
				} else {
					// if specializationId doesn't exist in db, then no data would be returned for that
					// particular specializationId
					if (dataOfSingleSubStreamInResponse == null) {
						Reporter.log(", No data returned for specializationId : " + specializationId + " [Correct]", true);
					} else {
						Reporter.log(", InCorrect data of specializationId : " + specializationId + " ["
								+ dataOfSingleSubStreamInResponse + "]", true);
						Assert.fail("InCorrect data of specializationId : " + specializationId + " ["
								+ dataOfSingleSubStreamInResponse + "]");
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
			Assert.fail("SQLException occured :" + e.getMessage());
		}
	}

	@Test(priority = 2)
	public static void respnseTimeStats_GetMultipleSpecilization() {
		respnseTimeStatsCommon(responseTimes, apiPath.substring(apiPath.lastIndexOf("/") + 1));
	}
}
