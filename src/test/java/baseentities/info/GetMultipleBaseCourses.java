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

public class GetMultipleBaseCourses extends Common {
	static String apiPath;
	String api;
	static String[] params;
	Response apiResponse = null;
	String apiResponseMsgExpected;
	static String testDataFilePath;
	int basecourseIds;

	static ArrayList<Integer> responseTimes = new ArrayList<Integer>();

	@BeforeClass
	public void doItBeforeTest() {
		// apiResponseExpectedTime = 200; // in MS
		loadPropertiesFromConfig("baseentities");
		// api path
		apiPath = "baseentities/api/v1/info/getMultipleBaseCourses";
		api = serverPath + apiPath;
		// API params in sequence followed by apiResponseMsgExpected
		params = new String[] { "basecourseIds", "apiResponseMsgExpected" };
		// test data file
		testDataFilePath = "//src//test//resources//baseentities//info//GetMultipleBaseCourse.xlsx";

	}

	@DataProvider
	public static String[][] testData() throws Exception {
		// The number of times data is repeated, test will be executed the same no. of
		// times
		String[][] testData = prepareTestData(params, testDataFilePath);
		return testData;
	}

	@Test(dataProvider = "testData", priority = 1)
	public void verifyGetMultipleBaseCourseApi(String basecourseIds, String apiResponseMsgExpected) {
		this.apiResponseMsgExpected = apiResponseMsgExpected;

		// pass api params and hit api
		if (basecourseIds.equals("ignoreHeader"))
			apiResponse = RestAssured.given().when().post(api).then().extract().response();
		else if (basecourseIds.equals("ignoreInput"))
			apiResponse = RestAssured.given().header("AUTHREQUEST", "INFOEDGE_SHIKSHA").when().post(api).then()
					.extract().response();
		else if (!basecourseIds.equals("ignoreInput"))
			apiResponse = RestAssured.given().header("AUTHREQUEST", "INFOEDGE_SHIKSHA")
					.param("basecourseIds[]", basecourseIds).when().post(api).then().extract().response();

		// fetch status code from response
		int statusCode = apiResponse.getStatusCode();
		JsonPath jsonPath = apiResponse.jsonPath();

		if (statusCode == 200) { // Positive case
			Reporter.log("Positive case", true);
			verifyPostiveCases(basecourseIds, jsonPath);
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

	public void verifyPostiveCases(String primarybasecourseIds, JsonPath jsonPath) {
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
			String[] primarybasecourseIdsArr = primarybasecourseIds.split(",");
			for (int i = 0; i < primarybasecourseIdsArr.length; i++) {
				this.basecourseIds = Integer.parseInt(primarybasecourseIdsArr[i]);

				// fetch data of specializationId from api response [jsonPath]
				String dataOfSingleSubStreamInResponse = jsonPath.getString("data." + basecourseIds);

				// fetch details from specializationId
				ResultSet rs = exceuteDbQuery(
						"SELECT base_course_id, name, alias, synonym, level, credential_1, credential_2, is_dummy, is_popular, is_hyperlocal, is_executive FROM base_courses where status = 'live' and base_course_id = "
								+ basecourseIds + ";",
						"shiksha");

				// get row count of resultSet of sql query
				int rowcount = getCountOfResultSet(rs);

				if (rowcount > 0) {

					rs.next();

					// verify specializationId in data[response] as same in db
					int basecourseIdsOfSingleBaseCourseInDataResponse = jsonPath
							.getInt("data." + basecourseIds + ".baseCourseId");
					if (basecourseIdsOfSingleBaseCourseInDataResponse == basecourseIds) {
						Reporter.log(", Correct value of baseCourseId [" + basecourseIdsOfSingleBaseCourseInDataResponse
								+ "] for BaseCourseId : " + basecourseIds + "", true);
					} else {
						Reporter.log(", InCorrect value of BaseCourseId for BaseCourseId: " + basecourseIds + " ["
								+ basecourseIdsOfSingleBaseCourseInDataResponse + "]", true);
						Assert.fail("InCorrect value of baseCourseId for BaseCourseId : " + basecourseIds + " ["
								+ basecourseIdsOfSingleBaseCourseInDataResponse + "]");
					}

					// verify name in data[response] as same in db
					String baseCourseNameDb = rs.getString("name");
					String nameOfSingleBaseCourseInDataResponse = jsonPath.getString("data." + basecourseIds + ".name");
					if (nameOfSingleBaseCourseInDataResponse.equalsIgnoreCase(baseCourseNameDb)) {
						Reporter.log(", Correct name [" + nameOfSingleBaseCourseInDataResponse + "] of BaseCourseId : "
								+ basecourseIds + "", true);
					} else {
						Reporter.log(", InCorrect name of BaseCourseNameDb: " + basecourseIds + " ["
								+ nameOfSingleBaseCourseInDataResponse + "]", true);
						Assert.fail("InCorrect name of BaseCourseId : " + basecourseIds + " ["
								+ nameOfSingleBaseCourseInDataResponse + "]");
					}

					// verify alias in data[response] as same in db
					String aliasDb = rs.getString("alias");
					if (aliasDb.equals(""))
						aliasDb = null;
					String aliasOfSingleBaseCourseInDataResponse = jsonPath.getString("data." + basecourseIds + ".alias");
					if ((aliasDb == null && aliasOfSingleBaseCourseInDataResponse == aliasDb) || (aliasOfSingleBaseCourseInDataResponse.equalsIgnoreCase(aliasDb))) {
						Reporter.log(", Correct alias [" + aliasOfSingleBaseCourseInDataResponse + "] of BaseCourseId : "
								+ basecourseIds + "", true);
					} else {
						Reporter.log(", InCorrect alias of BaseCourseId: " + basecourseIds + " ["
								+ aliasOfSingleBaseCourseInDataResponse + "]", true);
						Assert.fail("InCorrect alias of BaseCourseId : " + basecourseIds + " ["
								+ aliasOfSingleBaseCourseInDataResponse + "]");
					}

					// verify synonym in data[response] as same in db
					String synonymsDb = "[" + rs.getString("synonym") + "]";
					String synonymsOfSingleBaseCourseInDataResponse = jsonPath
							.getString("data." + basecourseIds + ".synonym");
					if (synonymsOfSingleBaseCourseInDataResponse.equalsIgnoreCase(synonymsDb)) {
						Reporter.log(", Correct synonyms [" + synonymsOfSingleBaseCourseInDataResponse
								+ "] of BaseCourseId : " + basecourseIds + "", true);
					} else {
						Reporter.log(", InCorrect synonyms of BaseCourseId: " + basecourseIds + " ["
								+ synonymsOfSingleBaseCourseInDataResponse + "]", true);
						Assert.fail("InCorrect synonyms of BaseCourseId : " + basecourseIds + " ["
								+ synonymsOfSingleBaseCourseInDataResponse + "]");
					}

					//verify type in data[response] as same in db
					String LevelIdDb = rs.getString("level");
					String LeveIdOfBaseCourseInDataResponse = jsonPath
							.getString("data." + basecourseIds + ".level");
					if (LeveIdOfBaseCourseInDataResponse.equalsIgnoreCase(LevelIdDb)) {
						Reporter.log(", Correct type [" + LeveIdOfBaseCourseInDataResponse
								+ "] of BaseCourseId : " + basecourseIds + "", true);
					} else {
						Reporter.log("InCorrect LevelId of BaseCourseId: " + basecourseIds + " ["
								+ LeveIdOfBaseCourseInDataResponse + "]", true);
						Assert.fail("InCorrect LeveId of BaseCourseId : " + basecourseIds + " ["
								+ LeveIdOfBaseCourseInDataResponse + "]");
					}
					
					//verify isDummyld in data[response] as same in db
					String isDummyIdDb = rs.getString("is_dummy");
					String isDummyIdOfSingleBaseCourseInDataResponse = jsonPath
							.getString("data." + basecourseIds + ".isDummy");
					if (isDummyIdOfSingleBaseCourseInDataResponse.equalsIgnoreCase(isDummyIdDb)) {
						Reporter.log(", Correct DummyId [" + isDummyIdOfSingleBaseCourseInDataResponse
								+ "] of BaseCourseId : " + basecourseIds + "", true);
					} else {
						Reporter.log(", InCorrect DummyId of BaseCourseId: " + basecourseIds + " ["
								+ isDummyIdOfSingleBaseCourseInDataResponse + "]", true);
						Assert.fail("InCorrect DummyId of BaseCourseId : " + basecourseIds + " ["
								+ isDummyIdOfSingleBaseCourseInDataResponse + "]");
					}
					
					//verify isPopularId in data[response] as same in db
					String isPopularIdDb = rs.getString("is_popular");
					String isPopularIdOfSingleBaseCourseInDataResponse = jsonPath
							.getString("data." + basecourseIds + ".isPopular");
					if (isPopularIdOfSingleBaseCourseInDataResponse.equalsIgnoreCase(isPopularIdDb)) {
						Reporter.log(", Correct isPopularId [" + isPopularIdOfSingleBaseCourseInDataResponse
								+ "] of BaseCourseId : " + basecourseIds + "", true);
					} else {
						Reporter.log(", InCorrect isPopularId of BaseCourseId: " + basecourseIds + " ["
								+ isPopularIdOfSingleBaseCourseInDataResponse + "]", true);
						Assert.fail("InCorrect idPopularId of BaseCourseId : " + basecourseIds + " ["
								+ isPopularIdOfSingleBaseCourseInDataResponse + "]");
					}
					
					//Verify isHyperlocalId in data[response] as same in db
					String isHyperlocalIdDb = rs.getString("is_hyperlocal");
					String isHyperlocalIdOfSingleBaseCourseInDataResponse = jsonPath
							.getString("data." + basecourseIds + ".isHyperlocal");
					if (isHyperlocalIdOfSingleBaseCourseInDataResponse.equalsIgnoreCase(isHyperlocalIdDb)) {
						Reporter.log(", Correct isHyperlocalId [" + isHyperlocalIdOfSingleBaseCourseInDataResponse
								+ "] of BaseCourseId : " + basecourseIds + "", true);
					} else {
						Reporter.log(", InCorrect isHyperlocalId of BaseCourseId: " + basecourseIds + " ["
								+ isHyperlocalIdOfSingleBaseCourseInDataResponse + "]", true);
						Assert.fail("InCorrect isHyperlocalId of BaseCourseId : " + basecourseIds + " ["
								+ isHyperlocalIdOfSingleBaseCourseInDataResponse + "]");
					}
					
					//Verify isExecutiveId in data[response] as same in db
					String isExecutiveIdDb = rs.getString("is_executive");
					String isExecutiveIdOfSingleBaseCourseInDataResponse = jsonPath
							.getString("data." + basecourseIds + ".isExecutive");
					if (isExecutiveIdOfSingleBaseCourseInDataResponse.equalsIgnoreCase(isExecutiveIdDb)) {
						Reporter.log(", Correct isExecutiveId [" + isExecutiveIdOfSingleBaseCourseInDataResponse
								+ "] of BaseCourseId : " + basecourseIds + "", true);
					} else {
						Reporter.log(", InCorrect isExecutiveId of BaseCourseId: " + basecourseIds + " ["
								+ isExecutiveIdOfSingleBaseCourseInDataResponse + "]", true);
						Assert.fail("InCorrect isExecutiveId of BaseCourseId : " + basecourseIds + " ["
								+ isExecutiveIdOfSingleBaseCourseInDataResponse + "]");
					}
					
					String credentialId1 =  rs.getString("credential_1");
					String credentialId2 =  rs.getString("credential_2");
					
					if(credentialId2 == null){
						credentialId2 = "0";
					}
					String credentialId;
					if(credentialId2.equals("0")){
						credentialId = "[" + credentialId1 + "]";
					}
					else{
						credentialId = "[" + credentialId1 +", "+ credentialId2 + "]";
					}
					String CredentialsOfSingleBaseCourseInDataResponse = jsonPath
							.getString("data." + basecourseIds + ".credential");
					if (CredentialsOfSingleBaseCourseInDataResponse.equalsIgnoreCase(credentialId)) {
						Reporter.log(", Correct CredentialId [" + CredentialsOfSingleBaseCourseInDataResponse
								+ "] of baseCourseId : " + basecourseIds + "", true);
					} else {
						System.out.println("Credentials if failed"+credentialId);
						Reporter.log(", InCorrect CredentialId of baseCourseId: " + basecourseIds + " ["
								+ CredentialsOfSingleBaseCourseInDataResponse + "]", true);
						Assert.fail("InCorrect CredentialId of baseCourseId : " + basecourseIds + " ["
								+ CredentialsOfSingleBaseCourseInDataResponse + "]");
					}
					
				} else {
					// if specializationId doesn't exist in db, then no data would be returned for that
					// particular specializationId
					if (dataOfSingleSubStreamInResponse == null) {
						Reporter.log(", No data returned for specializationId : " + basecourseIds + " [Correct]", true);
					} else {
						Reporter.log(", InCorrect data of specializationId : " + basecourseIds + " ["
								+ dataOfSingleSubStreamInResponse + "]", true);
						Assert.fail("InCorrect data of specializationId : " + basecourseIds + " ["
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
