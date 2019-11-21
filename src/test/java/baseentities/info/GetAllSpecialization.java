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

public class GetAllSpecialization extends Common{

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
		apiPath = "baseentities/api/v1/info/getAllSpecializations";
		api = serverPath + apiPath;
		// API params in sequence followed by apiResponseMsgExpected
		params = new String[] { "specializationIds", "apiResponseMsgExpected" };
		// test data file
		testDataFilePath = "//src//test//resources//baseentities//info//GetAllSpecializations.xlsx";

	}

	@DataProvider
	public static String[][] testData() throws Exception {
		// The number of times data is repeated, test will be executed the same no. of
		// times
		String[][] testData = prepareTestData(params, testDataFilePath);
		return testData;
	}

	@Test(dataProvider = "testData", priority = 1)
	public void verifyGetAllSpecializationApi(String specializationIds, String apiResponseMsgExpected) {
		this.apiResponseMsgExpected = apiResponseMsgExpected;

		// pass api params and hit api
		if (specializationIds.equals("ignoreHeader"))
			apiResponse = RestAssured.given().when().post(api).then().extract().response();
		else if (specializationIds.equals("ignoreInput"))
			apiResponse = RestAssured.given().header("AUTHREQUEST", "INFOEDGE_SHIKSHA").when().post(api).then()
					.extract().response();

		// fetch status code from response
		int statusCode = apiResponse.getStatusCode();
		JsonPath jsonPath = apiResponse.jsonPath();

		if (statusCode == 200) { // Positive case
			Reporter.log("Positive case", true);
			verifyPostiveCases(jsonPath);
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

	public void verifyPostiveCases(JsonPath jsonPath) {
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

			// fetch all substream details from db
			ResultSet rs = exceuteDbQuery(
					"SELECT specialization_id, name, primary_stream_id, primary_substream_id, alias, synonym, type FROM specializations where status = 'live';",
						"shiksha");

			// get row count of resultSet of sql query
			int rowcount = getCountOfResultSet(rs);

			if (rowcount > 0) {
				while (rs.next()) {
					// verify substream_id in data[response] as same in db
					this.specializationId = rs.getInt("specialization_id");
					int subStreamIdOfSingleSpecializationInDataResponse = jsonPath
							.getInt("data." + specializationId + ".specializationId");
					if (subStreamIdOfSingleSpecializationInDataResponse == specializationId) {
						Reporter.log(", Correct value of specializationId [" + subStreamIdOfSingleSpecializationInDataResponse
								+ "] for specializationId : " + specializationId + "", true);
					} else {
						Reporter.log(", InCorrect value of specializationId for specializationId: " + specializationId + " ["
								+ subStreamIdOfSingleSpecializationInDataResponse + "]", true);
						Assert.fail("InCorrect value of specializationId for specializationId : " + specializationId + " ["
								+ subStreamIdOfSingleSpecializationInDataResponse + "]");
					}

					// verify name in data[response] as same in db
					String specializationNameDb = rs.getString("name");
					String nameOfSingleSpecializationInDataResponse = jsonPath.getString("data." + specializationId + ".name");
					if (nameOfSingleSpecializationInDataResponse.equalsIgnoreCase(specializationNameDb)) {
						Reporter.log(", Correct name [" + nameOfSingleSpecializationInDataResponse + "] of specialization : "
								+ specializationId + "", true);
					} else {
						Reporter.log(", InCorrect name of specializationId: " + specializationId + " ["
								+ nameOfSingleSpecializationInDataResponse + "]", true);
						Assert.fail("InCorrect name of specializationId : " + specializationId + " ["
								+ nameOfSingleSpecializationInDataResponse + "]");
					}

					// verify alias in data[response] as same in db
					String aliasDb = rs.getString("alias");
					if (aliasDb.equals(""))
						aliasDb = null;
					String aliasOfSingleSubStreamInDataResponse = jsonPath.getString("data." + specializationId + ".alias");
					if ((aliasDb == null && aliasOfSingleSubStreamInDataResponse == aliasDb) || (aliasOfSingleSubStreamInDataResponse.equalsIgnoreCase(aliasDb))) {
						Reporter.log(", Correct alias [" + aliasOfSingleSubStreamInDataResponse + "] of specializationId : "
								+ specializationId + "", true);
					} else {
						Reporter.log(", InCorrect alias of specializationId: " + specializationId + " ["
								+ aliasOfSingleSubStreamInDataResponse + "]", true);
						Assert.fail("InCorrect alias of specializationId : " + specializationId + " ["
								+ aliasOfSingleSubStreamInDataResponse + "]");
					}

					// verify synonyms in data[response] as same in db
					String synonymsDb = "[" + rs.getString("synonym") + "]";
					String synonymsOfSingleSubStreamInDataResponse = jsonPath
							.getString("data." + specializationId + ".synonym");
					if (synonymsOfSingleSubStreamInDataResponse.equalsIgnoreCase(synonymsDb)) {
						Reporter.log(", Correct synonyms [" + synonymsOfSingleSubStreamInDataResponse
								+ "] of specializationId : " + specializationId + "", true);
					} else {
						Reporter.log(", InCorrect synonyms of specializationId: " + specializationId + " ["
								+ synonymsOfSingleSubStreamInDataResponse + "]", true);
						Assert.fail("InCorrect synonyms of specializationId : " + specializationId + " ["
								+ synonymsOfSingleSubStreamInDataResponse + "]");
					}

					// verify primaryStreamId in data[response] as same in db
					String primaryStreamIdDb = rs.getString("primary_stream_id");
					String primaryStreamIdOfSingleSubStreamInDataResponse = jsonPath
							.getString("data." + specializationId + ".primaryStreamId");
					if (primaryStreamIdOfSingleSubStreamInDataResponse.equalsIgnoreCase(primaryStreamIdDb)) {
						Reporter.log(", Correct primaryStreamId [" + primaryStreamIdOfSingleSubStreamInDataResponse
								+ "] of specializationId : " + specializationId + "", true);
					} else {
						Reporter.log(", InCorrect primaryStreamId of specializationId: " + specializationId + " ["
								+ primaryStreamIdOfSingleSubStreamInDataResponse + "]", true);
						Assert.fail("InCorrect primaryStreamId of specializationId : " + specializationId + " ["
								+ primaryStreamIdOfSingleSubStreamInDataResponse + "]");
					}
//					verify primarySubStreamId in data[response] as same in db
					String primarySubStreamIdDb = rs.getString("primary_substream_id");
					String primarySubStreamIdOfSingleSpecializationInDataResponse = jsonPath
							.getString("data." + specializationId + ".primarySubstreamId");
					if (primarySubStreamIdOfSingleSpecializationInDataResponse.equalsIgnoreCase(primarySubStreamIdDb)) {
						Reporter.log(", Correct primarySubStreamId [" + primarySubStreamIdOfSingleSpecializationInDataResponse
								+ "] of specializationId : " + specializationId + "", true);
					} else {
						Reporter.log(", InCorrect primarySubStreamId of specializationId: " + specializationId + " ["
								+ primarySubStreamIdOfSingleSpecializationInDataResponse + "]", true);
						Assert.fail("InCorrect primarySubStreamId of specializationId : " + specializationId + " ["
								+ primarySubStreamIdOfSingleSpecializationInDataResponse + "]");
					}
//					verify type in data[response] as same in db
					String typeDb = rs.getString("type");
					String primarytypeOfSingleSpecializationInDataResponse = jsonPath
							.getString("data." + specializationId + ".type");
					if (primarytypeOfSingleSpecializationInDataResponse.equalsIgnoreCase(typeDb)) {
						Reporter.log(", Correct type [" + primarytypeOfSingleSpecializationInDataResponse
								+ "] of specializationId : " + specializationId + "", true);
					} else {
						Reporter.log(", InCorrect type of specializationId: " + specializationId + " ["
								+ primarytypeOfSingleSpecializationInDataResponse + "]", true);
						Assert.fail("InCorrect type of specializationId : " + specializationId + " ["
								+ primarytypeOfSingleSpecializationInDataResponse + "]");
					}

				}
			} else {
				// if no details fetched from db
				Assert.fail("Unable to fetch data from db");
			}
		} catch (SQLException e) {
			e.printStackTrace();
			Assert.fail("SQLException occured :" + e.getMessage());
		}
	}

	@Test(priority = 2)
	public static void respnseTimeStats_GetAllSpecialization() {
		respnseTimeStatsCommon(responseTimes, apiPath.substring(apiPath.lastIndexOf("/") + 1));
	}
}
