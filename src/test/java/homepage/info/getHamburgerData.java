package homepage.info;

import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import common.Common;

public class getHamburgerData extends Common {
	static String apiPath;
	String api;
	static String[] params;
	Response apiResponse = null;
	String apiResponseMsgExpected;
	static String testDataFilePath;
	static ArrayList<Integer> responseTimes = new ArrayList<Integer>();
	boolean result = false;
	int dbStreamId, dbSubStreamId, dbSpecializationId;
	String path;
	ArrayList<String> dbList;
	List<String> jsonnamelist;
	List<String> jsonurllist;

	@BeforeClass
	public void doItBeforeTest() throws FileNotFoundException {
		// apiResponseExpectedTime = 200; // in MS
		loadPropertiesFromConfig("apifacade");
		// api path
		apiPath = "apigateway/commonapi/v1/info/getHamburgerData";
		api = serverPath + apiPath;
		// API params in sequence followed by apiResponseMsgExpected
		params = new String[] { "Ids", "apiResponseMsgExpected" };
		// test data file
		testDataFilePath = "//src//test//resources//homepage//info//getHamburgerData.xlsx";
		PrintWriter pw = new PrintWriter("Logs//GetHamburgerData.txt");
		pw.close();
		pw = new PrintWriter("Logs//HamburgerDataNotFound.txt");
		pw.close();
	}

	@DataProvider
	public static String[][] testData() throws Exception {
		// The number of times data is repeated, test will be executed the same
		// no. of
		// times
		String[][] testData = prepareTestData(params, testDataFilePath);
		return testData;
	}

	@Test(dataProvider = "testData", priority = 1)
	public void verifyGetHamburgerDataApi(String Ids,
			String apiResponseMsgExpected) throws IOException {
		this.apiResponseMsgExpected = apiResponseMsgExpected;

		// pass api params and hit apicompareCourseId
		if (Ids.equals("ignoreHeader")) {
			return;
			// apiResponse = RestAssured.given().param("userId",
			// userId).param("compareCourseIds[]",compareCourseId
			// ).when().post(api).then().extract().response();
		} else if (Ids.equals("ignoreInput")) {
			apiResponse = RestAssured.given()
					.header("AUTHREQUEST", "INFOEDGE_SHIKSHA").when().get(api)
					.then().extract().response();
		}

		// fetch status code from response
		int statusCode = apiResponse.getStatusCode();
		JsonPath jsonPath = apiResponse.jsonPath();

		if (statusCode == 200) { // Positive case
			Reporter.log("Positive case", true);
			verifyPostiveCases(jsonPath);
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

		long apiResponseActualTime = apiResponse
				.getTimeIn(TimeUnit.MILLISECONDS);
		responseTimes.add((int) apiResponseActualTime);

		if (apiResponseActualTime > apiResponseExpectedTime) {
			Reporter.log("<br> API Response time : " + apiResponseActualTime
					+ " MS is greater than expected time ("
					+ apiResponseExpectedTime + " MS)", true);
			Assert.fail("API Response time : " + apiResponseActualTime
					+ " MS is greater than expected time ("
					+ apiResponseExpectedTime + " MS)");
		} else {
			Reporter.log("<br> API Response time : " + apiResponseActualTime
					+ " MS", true);
		}
	}

	public void verifyPostiveCases(JsonPath jsonPath) throws IOException {
		try {
			// verify status is success
			String statusResponse = jsonPath.getString("status");
			if (statusResponse.contentEquals("success")) {
				Reporter.log(", Correct status [success]", true);
			} else {
				Reporter.log(", InCorrect status [" + statusResponse + "]",
						true);
				Assert.fail("InCorrect status [" + statusResponse + "]");
			}

			// verify message is null
			String messageResponse = jsonPath.getString("message");
			if (messageResponse == null) {
				Reporter.log(", Correct message [null]", true);
			} else {
				Reporter.log(", InCorrect message [" + messageResponse + "]",
						true);
				Assert.fail("InCorrect message [" + messageResponse + "]");
			}
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("SQLException occured :" + e.getMessage());
		}
		path = new java.io.File(".").getCanonicalPath();
		boolean result = verifyhierarchiesHavingNonZeroListings(jsonPath);
		Assert.assertTrue(!result,
				"verifyhierarchiesHavingNonZeroListings Test Case Failed");
		result = verifybaseCoursesHavingNonZeroListings(jsonPath);
		Assert.assertTrue(!result,
				"verifybaseCoursesHavingNonZeroListings Test Case Failed");
		result = verifyRankPredictor(jsonPath);
		Assert.assertTrue(!result, "verifyRankPredictor Test Case Failed");
		result = verifyGetExamList(jsonPath);
		Assert.assertTrue(!result, "VerifyGetExamList Test Case Failed");
		result = verifyCollegePredictor(jsonPath);
		Assert.assertTrue(!result, "verifyCollegePredictor Test Case Failed");
		result = verifyexamHierarchyData(jsonPath);
		Assert.assertTrue(!result, "verifyexamHierarchyData Test Case Failed");
		result = verifyRankingStreamWise(jsonPath);
		Assert.assertTrue(!result, "verifyRankingStreamWise Test Case Failed");
		result = verifycampusRepProgramDetails(jsonPath);
		Assert.assertTrue(!result, "verifycampusRepProgramDetails Test Case Failed");
	}

	public boolean verifyhierarchiesHavingNonZeroListings(JsonPath jsonpath) {
		filewriter("verifyhierarchiesHavingNonZeroListings", "Logs//GetHamburgerData");
		result = false;
		ResultSet rsBasehierarchies = exceuteDbQuery(
				"select distinct(stream_id) from base_hierarchies where status = 'live';",
				"shiksha");
		int baseHierarchiesrowCount = getCountOfResultSet(rsBasehierarchies);

		try {
			if (baseHierarchiesrowCount > 0) {
				while (rsBasehierarchies.next()) {
					dbStreamId = rsBasehierarchies.getInt("stream_id");
					ResultSet rsStream = exceuteDbQuery(
							"select name from streams where stream_id ="
									+ dbStreamId + ";", "shiksha");
					while (rsStream.next()) {
						String dbStreamName = rsStream.getString("name");

						int jsonStreamId = Integer
								.parseInt(jsonpath
										.getString("data.hierarchiesHavingNonZeroListings."
												+ dbStreamId + ".id"));
						String jsonStreamName = jsonpath
								.getString("data.hierarchiesHavingNonZeroListings."
										+ dbStreamId + ".name");
						if (jsonStreamName.equalsIgnoreCase(dbStreamName)
								&& dbStreamId == jsonStreamId) {
							System.out
									.println("Stream id and name is appear Correct for Stream id: "
											+ dbStreamId);
						} else {
							System.out
									.println("Stream id and name is not appear Correct for Stream id: "
											+ dbStreamId);
							System.out.println("Expected Result: Stream Id: "
									+ dbStreamId + " Stream Name: "
									+ dbStreamName);
							System.out.println("Actual Result: Stream Id: "
									+ jsonStreamId + " Stream Name: "
									+ jsonStreamName);
							filewriter("Expected Result: Stream Id: "
									+ dbStreamId + " Stream Name: "
									+ dbStreamName, "Logs//GetHamburgerData");
							filewriter("Actual Result: Stream Id: "
									+ jsonStreamId + " Stream Name: "
									+ jsonStreamName, "Logs//GetHamburgerData");
							result = true;
						}
					}
				}
			}
			ResultSet rsSubStream = exceuteDbQuery(
					"select distinct(substream_id) from shiksha_courses_type_information where primary_hierarchy = 1 and status = 'live' ; ",
					"shiksha");
			int SubStreamRowCount = getCountOfResultSet(rsSubStream);
			for (int j = 1; j <= SubStreamRowCount; j++) {
				while (rsSubStream.next()) {
					int dbSubStreamId = rsSubStream.getInt("substream_id");
					ResultSet rsSubStreamName = exceuteDbQuery(
							"select bh.stream_id, ss.name from base_hierarchies as bh join substreams as ss where bh.substream_id = "
									+ dbSubStreamId
									+ " and ss.substream_id ="
									+ dbSubStreamId
									+ " and bh.status = 'live' and ss.status= 'live' limit 1; ",
							"shiksha");
					while (rsSubStreamName.next()) {
						String dbSubStreamName = rsSubStreamName
								.getString("name");
						if (dbSubStreamName.contains("&")) {
							dbSubStreamName = dbSubStreamName.replace("&",
									"and");
						}
						if (dbSubStreamName.contains("-")) {
							dbSubStreamName = dbSubStreamName.replace("-", " ");
						}
						if (dbSubStreamName.contains(",")) {
							dbSubStreamName = dbSubStreamName.replace(",", "");
						}
						if (dbSubStreamName.contains(".")) {
							dbSubStreamName = dbSubStreamName.replace(".", "");
						}
						if (dbSubStreamName.contains(" ")) {
							dbSubStreamName = dbSubStreamName.replace(" ", "");
						}

						int dbStreamId = rsSubStreamName.getInt("stream_id");
						try {
							String jsonSubStreamName = jsonpath
									.getString("data.hierarchiesHavingNonZeroListings."
											+ dbStreamId
											+ ".substreams."
											+ dbSubStreamId + ".name");
							if (jsonSubStreamName.contains(" ")) {
								jsonSubStreamName = jsonSubStreamName.replace(
										" ", "");
							}
							int jsonSubStreamId = Integer
									.parseInt(jsonpath
											.getString("data.hierarchiesHavingNonZeroListings."
													+ dbStreamId
													+ ".substreams."
													+ dbSubStreamId + ".id"));
							if (dbSubStreamName
									.equalsIgnoreCase(jsonSubStreamName)
									&& dbSubStreamId == jsonSubStreamId) {
								System.out
										.println("SubStream name and Id is appear correct for Stream id: "
												+ dbStreamId
												+ " and Substream Id: "
												+ dbSubStreamId);
							} else {
								System.out
										.println("SubStream name and Id is not appear correct for Stream id: "
												+ dbStreamId
												+ " and Substream Id: "
												+ dbSubStreamId);
								System.out
										.println("Expected Result: SubStreamId: "
												+ dbSubStreamId
												+ " SubStream Name: "
												+ dbSubStreamName);
								System.out
										.println("Actual Result: SubStreamId: "
												+ jsonSubStreamId
												+ " SubStream Name: "
												+ jsonSubStreamName);
								filewriter("Expected Result: SubStreamId: "
										+ dbSubStreamId + " SubStream Name: "
										+ dbSubStreamName, "Logs//GetHamburgerData");
								filewriter("Actual Result: SubStreamId: "
										+ jsonSubStreamId + " SubStream Name: "
										+ jsonSubStreamName, "Logs//GetHamburgerData");
								result = true;
							}
						} catch (Exception e) {
							System.out
									.println("SubStream name and Id is not appear in Json for Stream id: "
											+ dbStreamId
											+ " and Substream Id: "
											+ dbSubStreamId);
							filewriter(
									"Data not found in json file for mentioned result",
									"Logs//HamburgerDataNotFound");
							filewriter("Expected Result: SubStreamId: "
									+ dbSubStreamId + " SubStream Name: "
									+ dbSubStreamName, "Logs//HamburgerDataNotFound");
							continue;
						}
					}
				}
			}
			ResultSet rsSpeciazliation = exceuteDbQuery(
					"select distinct(specialization_id), stream_id, substream_id from shiksha_courses_type_information where primary_hierarchy = 1 and status = 'live'and base_course !=0;",
					"shiksha");
			int SpecializationrowCount = getCountOfResultSet(rsSpeciazliation);
			for (int k = 1; k <= SpecializationrowCount; k++) {
				if (rsSpeciazliation.next()) {
					int dbspecializationId = rsSpeciazliation
							.getInt("specialization_id");
					int dbsubstreamId = rsSpeciazliation.getInt("substream_id");
					int dbStreamId = rsSpeciazliation.getInt("stream_id");
					ResultSet rsSpecializatioName = exceuteDbQuery(
							"select name from specializations where specialization_id ="
									+ dbspecializationId
									+ " and status= 'live'; ", "shiksha");
					while (rsSpecializatioName.next()) {

						String jsonspeacPath;
						if (dbspecializationId != 0) {
							if (dbsubstreamId == 0) {
								jsonspeacPath = "data.hierarchiesHavingNonZeroListings."
										+ dbStreamId
										+ ".specializations."
										+ dbspecializationId;
							} else {
								jsonspeacPath = "data.hierarchiesHavingNonZeroListings."
										+ dbStreamId
										+ ".substreams."
										+ dbsubstreamId
										+ ".specializations."
										+ dbspecializationId;
							}
							String dbSpecializationName = rsSpecializatioName
									.getString("name");
							if (dbSpecializationName.contains("&")) {
								dbSpecializationName = dbSpecializationName
										.replace("&", "and");
							}
							if (dbSpecializationName.contains("-")) {
								dbSpecializationName = dbSpecializationName
										.replace("-", " ");
							}
							if (dbSpecializationName.contains(",")) {
								dbSpecializationName = dbSpecializationName
										.replace(",", "");
							}
							if (dbSpecializationName.contains(".")) {
								dbSpecializationName = dbSpecializationName
										.replace(".", "");
							}
							if (dbSpecializationName.contains("/")
									&& !dbSpecializationName.contains(" / ")) {
								dbSpecializationName = dbSpecializationName
										.replace("/", " / ");
							}
							if (dbSpecializationName.contains("#")) {
								dbSpecializationName = dbSpecializationName
										.replace("#", " ");
							}
							if (dbSpecializationName.contains(" ")) {
								dbSpecializationName = dbSpecializationName
										.replace(" ", "");
							}
							try {
								String jsonSpecializationName = jsonpath
										.getString(jsonspeacPath + ".name");
								if (jsonSpecializationName.contains(" ")) {
									jsonSpecializationName = jsonSpecializationName
											.replace(" ", "");
								}
								int jsonSpecializationId = Integer
										.parseInt(jsonpath
												.getString(jsonspeacPath
														+ ".id"));
								if (dbSpecializationName
										.equalsIgnoreCase(jsonSpecializationName)
										&& dbspecializationId == jsonSpecializationId) {
									System.out
											.println("SubStream name and Id is appear correct for Stream id: "
													+ dbStreamId
													+ " and SpecializationId: "
													+ dbspecializationId);
								} else {
									System.out
											.println("SubStream name and Id is not appear correct for Stream id: "
													+ dbStreamId
													+ " and SubStreamId "
													+ dbsubstreamId
													+ " Specialization Id: "
													+ dbspecializationId);
									System.out
											.println("Expected Result: SpecializationId: "
													+ dbspecializationId
													+ " Specialization Name: "
													+ dbSpecializationName);
									System.out
											.println("Actual Result: SpecializationId: "
													+ jsonSpecializationId
													+ " Specialization Name: "
													+ jsonSpecializationName);
									filewriter(
											"Expected Result: SpecializationId: "
													+ dbspecializationId
													+ " Specialization Name: "
													+ dbSpecializationName,
											"Logs//GetHamburgerData");
									filewriter(
											"Actual Result: SpecializationId: "
													+ jsonSpecializationId
													+ " Specialization Name: "
													+ jsonSpecializationName,
											"Logs//GetHamburgerData");
									result = true;
								}
							} catch (Exception e) {
								System.out
										.println("SubStream name and Id is not appear In json File for Stream id: "
												+ dbStreamId
												+ " and SubStreamId "
												+ dbsubstreamId
												+ "  Specialization Id: "
												+ dbspecializationId);
								filewriter("Data Not Found for Json File",
										"Logs//HamburgerDataNotFound");
								filewriter("StreamId: " + dbStreamId
										+ " SubStreamId: " + dbsubstreamId
										+ " SpecializationId: "
										+ dbspecializationId
										+ " Specialization Name: "
										+ dbSpecializationName,
										"Logs//HamburgerDataNotFound");
								continue;
							}
						}

					}
				}
			}
		} catch (Exception e) {

			e.printStackTrace();
		}
		return result;
	}

	public boolean verifybaseCoursesHavingNonZeroListings(JsonPath jsonpath) {
		filewriter("verifybaseCoursesHavingNonZeroListings", "Logs//GetHamburgerData");
		result = false;
		try {
			ResultSet rsStream = exceuteDbQuery(
					"Select * from streams where status = 'live';", "shiksha");
			while (rsStream.next()) {
				int StreamId = rsStream.getInt("stream_id");
				Map<Integer, String> jsonMap = new HashMap<Integer, String>(
						jsonpath.getMap("data.baseCoursesHavingNonZeroListings."
								+ StreamId));
				for (Map.Entry<Integer, String> entry : jsonMap.entrySet()) {
					int key = entry.getKey();
					String CourseName = entry.getValue();
					ResultSet rsBaseCourse = exceuteDbQuery(
							"select * from base_courses where base_course_id = "
									+ key + " and status = 'live';", "shiksha");
					while (rsBaseCourse.next()) {
						String BaseCourseName = rsBaseCourse.getString("name");
						if (BaseCourseName.contains(".")) {
							BaseCourseName = BaseCourseName.replace(".", "");
						}
						BaseCourseName = BaseCourseName.replace(" ", "");
						CourseName = CourseName.replace(" ", "");
						if (BaseCourseName.equalsIgnoreCase(CourseName)) {
							System.out
									.println("Course Name is appear correct for StreamId: "
											+ StreamId
											+ " BaseCourseId: "
											+ key);
						} else {
							System.out
									.println("Course Name is not appear correct for StreamId: "
											+ StreamId
											+ " BaseCourseId: "
											+ key);
							System.out.println("Expected Result: "
									+ BaseCourseName + " Actual Result: "
									+ CourseName);
							result = true;
						}
					}
				}
			}
		} catch (Exception e) {

		}
		return result;
	}

	public boolean verifyRankPredictor(JsonPath jsonPath) {
		filewriter("verifyRankPredictor", "Logs//GetHamburgerData");
		result = false;
		try {
			if (!jsonPath.getList("data.rankPredictorData.popularPredictor")
					.isEmpty()) {
				path = path + "\\src\\test\\resources\\predictor\\info\\"
						+ "rankPredictorConfig.properties";
				List<String> name = jsonPath
						.getList("data.rankPredictorData.popularPredictor.name");
				List<String> url = jsonPath
						.getList("data.rankPredictorData.popularPredictor.url");
				Map<String, String> jsonpopularMap = new HashMap<String, String>();
				Map<String, String> propMap = new HashMap<String, String>();
				for (int i = 0; i < name.size(); i++) {
					String predictorname = name.get(i);
					String predictorfullurl = url.get(i);
					String predictorUrl = predictorfullurl
							.substring(predictorfullurl.indexOf(".com/") + 5);
					if (jsonpopularMap.containsValue(predictorUrl)) {

					} else {
						jsonpopularMap.put(predictorname, predictorUrl);
					}
				}
				for (String key : jsonpopularMap.keySet()) {
					if (key.contains(" ")) {
						key = key.replace(" ", "-");
					}

					// Fetch Data from properties File
					Properties props = new Properties();
					props.load(new FileInputStream(path));
					for (Enumeration<?> e = props.propertyNames(); e
							.hasMoreElements();) {
						String propname = (String) e.nextElement();
						key = key.toLowerCase();
						// now you have name and value
						String propshortname = null;
						String prepurl = null;
						String propkey = null;
						propkey = "rankPredictorConfig.rpExams." + key;
						if (propname.startsWith(propkey)) {
							propshortname = props
									.getProperty(propkey + ".name");
							String propCollegeUrl = props.getProperty(propkey
									+ ".url");
							prepurl = "b-tech/resources/" + propCollegeUrl;
						}
						if (propshortname != null || prepurl != null) {
							propMap.put(propshortname, prepurl);
						}

					}
				}
				// Compare 2 Map of Json Data Map and Properties file Map
				if (jsonpopularMap.equals(propMap)) {
					System.out.println("Json Popular MAP: " + jsonpopularMap);
					System.out.println("Properties MAP: " + propMap);
					Assert.assertTrue(true, "Popular List Verified Sucessfully");
				} else {
					for (Map.Entry<String, String> entry : jsonpopularMap
							.entrySet()) {
						System.out.println("JsonpopularMap: " + entry);
						filewriter("JsonpopularMap: " + entry,
								"Logs//GetHamburgerData");
					}
					for (Map.Entry<String, String> entry : propMap.entrySet()) {
						filewriter("propMap: " + entry, "Logs//GetHamburgerData");
						System.out.println("propMap: " + entry);
					}
					Assert.assertTrue(false, "Popular List Verification Failed");

				}
			} else {
				System.out.println("Popular List is empty");
				filewriter("Popular List is empty", "Logs//GetHamburgerData");
			}
			// Other Json List
			if (!jsonPath.getList("data.rankPredictorData.otherPredictor")
					.isEmpty()) {
				List<String> name = jsonPath
						.getList("data.rankPredictorData.otherPredictor.name");
				List<String> url = jsonPath
						.getList("data.rankPredictorData.otherPredictor.url");
				Map<String, String> jsonotherMap = new HashMap<String, String>();
				Map<String, String> propMap = new HashMap<String, String>();
				for (int i = 0; i < name.size(); i++) {
					String predictorname = name.get(i);
					if (predictorname.contains(" ")) {
						predictorname = predictorname.replace(" ", "-");
					}
					String predictorfullurl = url.get(i);
					String predictorUrl = predictorfullurl
							.substring(predictorfullurl.indexOf(".com") + 4);
					if (jsonotherMap.containsValue(predictorUrl)) {

					} else {
						jsonotherMap.put(predictorname.toUpperCase(),
								predictorUrl);
					}
				}
				for (String key : jsonotherMap.keySet()) {

					Properties props = new Properties();
					props.load(new FileInputStream(path));
					for (Enumeration<?> e = props.propertyNames(); e
							.hasMoreElements();) {
						String propname = (String) e.nextElement();
						// now you have name and value
						if (key.equalsIgnoreCase("JEE-MAIN")) {
							key = "JEE-MAINS";
						}

						String propshortname = null;
						String prepurl = null;
						String propkey = null;
						propkey = "collegeConfig.cpExams." + key;
						if (propname.startsWith(propkey)) {
							propshortname = props.getProperty(propkey
									+ ".shortName");
							String propCollegeUrl = props.getProperty(propkey
									+ ".collegeUrl");
							String propdirectory = props.getProperty(propkey
									+ ".directoryName");
							prepurl = propdirectory + "/" + propCollegeUrl;
						}
						if (propshortname != null || prepurl != null) {
							propMap.put(propshortname, prepurl);
						}

					}
				}
				if (jsonotherMap.equals(propMap)) {
					System.out.println("Json Other MAP: " + jsonotherMap);
					System.out.println("Properties MAP: " + propMap);
					Assert.assertTrue(true, "Other List Verified Sucessfully");
				} else {
					for (Map.Entry<String, String> entry : jsonotherMap
							.entrySet()) {
						System.out.println("JsonotherMap: " + entry);
						filewriter("JsonotherMap: " + entry,
								"Logs//GetHamburgerData");
					}
					for (Map.Entry<String, String> entry : propMap.entrySet()) {
						System.out.println("propMap: " + entry);
						filewriter("propMap: " + entry, "Logs//GetHamburgerData");
					}
					Assert.assertTrue(false, "other List Verification Failed");

				}

			} else
				System.out.println("Other List is Empty"
						+ jsonPath.getList("data.otherList"));
			filewriter("Other List is Empty", "Logs//GetHamburgerData");
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("Exception occured :" + e.getMessage());
		}
		return result;
	}

	public boolean verifyGetExamList(JsonPath jsonPath) {
		filewriter("verifyGetExamList", "Logs//GetHamburgerData");
		result = false;
		try {
			ArrayList<HashMap<String, String>> popularjsonArray = new ArrayList<HashMap<String, String>>(
					jsonPath.getList("data.examList.popularExams"));
			ArrayList<HashMap<String, String>> otherjsonArray = new ArrayList<HashMap<String, String>>(
					jsonPath.getList("data.examList.otherExams"));
			ArrayList<HashMap<String, String>> jsonArray = new ArrayList<HashMap<String, String>>();
			jsonArray.addAll(popularjsonArray);
			jsonArray.addAll(otherjsonArray);

			ResultSet rs = exceuteDbQuery(
					"select distinct(epm.id), epm.name,epm.url from exampage_main as epm"
							+ " join exampage_master as epmas on epm.id = epmas.exam_id "
							+ "join examAttributeMapping as eam on epmas.exam_id = eam.examId"
							+ " join base_courses as bc on eam.entityId = bc.base_course_id "
							+ "where bc.alias = 'MBA' and eam.entityType ='course' and bc.status = 'live' and"
							+ " eam.status = 'live' and epm.status = 'live' and epmas.status = 'live';",
					"shiksha");
			// get row count of resultSet of sql query
			int rowcount = getCountOfResultSet(rs);
			dbList = new ArrayList<String>();
			if (rowcount > 0) {
				dbList.clear();
				while (rs.next()) {

					String name = null;

					jsonnamelist = new ArrayList<String>();
					jsonurllist = new ArrayList<String>();

					name = rs.getString("name");

					dbList.add(name);
				}
				for (int i = 0; i < jsonArray.size(); i++) {
					String s = jsonArray.get(i).get("url");
					s = s.substring(s.indexOf(".com/") + 5);
					if (s == "") {
						result = true;
//						Assert.assertTrue(false,
//								"String Appear to be null as no url passed from DB for Exam"
//										+ jsonArray.get(i).get("name"));
						result = true;
						filewriter(
								"String Appear to be null as no url passed from DB for Exam :"
										+ jsonArray.get(i).get("name"),
								"Logs//GetHamburgerData");
					}
				}

				for (int i = 0; i < jsonArray.size(); i++) {
					String s = jsonArray.get(i).get("name");
					jsonnamelist.add(s);
				}

				Collections.sort(jsonnamelist);
				Collections.sort(dbList);
				if (jsonnamelist.equals(dbList)) {

				} else {
					filewriter(" Json Array: " + jsonnamelist,
							"Logs//GetHamburgerData");
					filewriter(" DB Value Array: " + dbList,
							"Logs//GetHamburgerData");
					System.out
							.println("Test Case Execute Failed for GetExamList");
					result = true;
//					Assert.assertTrue(false,
//							"Test Execution Failed for GetexamList");
					result = true;
				}

			} else if (jsonPath.getList("data.examList.popularExams").isEmpty()
					&& jsonPath.getList("data.examList.otherExams").isEmpty()) {
				System.out.println("No Result Available for the same]");
				Assert.assertTrue(true, "No Result Available for the same");
			} else {
				filewriter(" Json Array: " + jsonnamelist,
						"Logs//GetHamburgerData");
				filewriter(" DB Value Array: " + dbList,
						"Logs//GetHamburgerData");
				System.out.println("Test Case Execute Failed for Exam");
//				Assert.assertTrue(false,
//						"Test Failed due to Sql return 0 Result: ");
				Reporter.log("Test Failed due to Sql return 0 Result",true);
				result = true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	public boolean verifyCollegePredictor(JsonPath jsonPath) {
		filewriter("verifyCollegePredictor", "Logs//GetHamburgerData");
		result = false;
		try {
			path = path + "\\src\\test\\resources\\predictor\\info\\"
					+ "collegePredictorConfig.properties";
			String[] Stream = new String[] { "Design", "Engineering" };
			String stream;
			for (int j = 0; j < Stream.length; j++) {
				stream = Stream[j];
				if (!jsonPath.getList("data." + stream + ".popularPredictor")
						.isEmpty()) {
					List<String> name = jsonPath.getList("data." + stream
							+ ".popularPredictor.name");
					List<String> url = jsonPath.getList("data." + stream
							+ ".popularPredictor.url");
					Map<String, String> jsonpopularMap = new HashMap<String, String>();
					Map<String, String> propMap = new HashMap<String, String>();
					for (int i = 0; i < name.size(); i++) {
						String predictorname = name.get(i);
						String predictorfullurl = url.get(i);
						String predictorUrl = predictorfullurl
								.substring(predictorfullurl.indexOf(".com") + 4);
						if (jsonpopularMap.containsValue(predictorUrl)) {

						} else {
							jsonpopularMap.put(predictorname, predictorUrl);
						}
					}
					for (String key : jsonpopularMap.keySet()) {
						if (key.contains(" ")) {
							key = key.replace(" ", "-");
							key = key.toUpperCase();
						}

						Properties props = new Properties();
						props.load(new FileInputStream(path));
						for (Enumeration<?> e = props.propertyNames(); e
								.hasMoreElements();) {
							String propname = (String) e.nextElement();
							// now you have name and value
							if (key.equalsIgnoreCase("JEE-MAIN")) {
								key = "JEE-MAINS";
							}

							String propshortname = null;
							String prepurl = null;
							String propkey = null;
							propkey = "collegeConfig.cpExams." + key;
							if (propname.startsWith(propkey)) {
								propshortname = props.getProperty(propkey
										+ ".shortName");
								String propCollegeUrl = props
										.getProperty(propkey + ".collegeUrl");
								String propdirectory = props
										.getProperty(propkey + ".directoryName");
								prepurl = propdirectory + "/" + propCollegeUrl;
							}
							if (propshortname != null || prepurl != null) {
								propMap.put(propshortname, prepurl);
							}

						}
					}
					if (jsonpopularMap.equals(propMap)) {
						System.out.println("Json Popular MAP: "
								+ jsonpopularMap);
						System.out.println("Properties MAP: " + propMap);
						Assert.assertTrue(true,
								"Popular List Verified Sucessfully");

					} else {
						for (Map.Entry<String, String> entry : jsonpopularMap
								.entrySet()) {
							filewriter("JsonpopularMap: " + entry,
									"Logs//GetHamburgerData");
						}
						for (Map.Entry<String, String> entry : propMap
								.entrySet()) {
							filewriter("propMap: " + entry,
									"Logs//GetHamburgerData");
						}
						result = true;
//						Assert.assertTrue(false,
//								"Popular List Verification Failed");

					}
				} else {
					filewriter("Popular List is empty",
							"Logs//GetHamburgerData");
				}

				if (!jsonPath.getList("data." + stream + ".otherPredictor")
						.isEmpty()) {
					List<String> name = jsonPath.getList("data." + stream
							+ ".otherPredictor.name");
					List<String> url = jsonPath.getList("data." + stream
							+ ".otherPredictor.url");
					Map<String, String> jsonotherMap = new HashMap<String, String>();
					Map<String, String> propMap = new HashMap<String, String>();
					for (int i = 0; i < name.size(); i++) {
						String predictorname = name.get(i);
						if (predictorname.contains(" ")) {
							predictorname = predictorname.replace(" ", "-");
						}
						String predictorfullurl = url.get(i);
						String predictorUrl = predictorfullurl
								.substring(predictorfullurl.indexOf(".com") + 4);
						if (jsonotherMap.containsValue(predictorUrl)) {

						} else {
							jsonotherMap.put(predictorname.toUpperCase(),
									predictorUrl);
						}
					}
					for (String key : jsonotherMap.keySet()) {

						Properties props = new Properties();
						props.load(new FileInputStream(path));
						for (Enumeration<?> e = props.propertyNames(); e
								.hasMoreElements();) {
							String propname = (String) e.nextElement();
							// now you have name and value
							if (key.equalsIgnoreCase("JEE-MAIN")) {
								key = "JEE-MAINS";
							}

							String propshortname = null;
							String prepurl = null;
							String propkey = null;
							propkey = "collegeConfig.cpExams." + key;
							if (propname.startsWith(propkey)) {
								propshortname = props.getProperty(propkey
										+ ".shortName");
								String propCollegeUrl = props
										.getProperty(propkey + ".collegeUrl");
								String propdirectory = props
										.getProperty(propkey + ".directoryName");
								prepurl = propdirectory + "/" + propCollegeUrl;
							}
							if (propshortname != null || prepurl != null) {
								propMap.put(propshortname, prepurl);
							}

						}
					}
					if (jsonotherMap.equals(propMap)) {
						System.out.println("Json Other MAP: " + jsonotherMap);
						System.out.println("Properties MAP: " + propMap);
						Assert.assertTrue(true,
								"Other List Verified Sucessfully");
					} else {
						for (Map.Entry<String, String> entry : jsonotherMap
								.entrySet()) {
							filewriter("JsonotherMap: " + entry,
									"Logs//GetHamburgerData");
						}
						for (Map.Entry<String, String> entry : propMap
								.entrySet()) {
							filewriter("propMap: " + entry,
									"Logs//GetHamburgerData");
						}
						result = true;
//						Assert.assertTrue(false,
//								"other List Verification Failed");

					}

				} else
					filewriter(
							"Other List is Empty"
									+ jsonPath.getList("data.otherList"),
							"Logs//GetHamburgerData");
			}
		} catch (Exception e) {
		}
		return result;
	}

	public boolean verifyexamHierarchyData(JsonPath jsonPath) {
		filewriter("verifyexamHierarchyData", "Logs//GetHamburgerData");
		result = false;
		try {
			// verify status is success
			String statusResponse = jsonPath.getString("status");
			if (statusResponse.contentEquals("success")) {
				Reporter.log(", Correct status [success]", true);
			} else {
				Reporter.log(", InCorrect status [" + statusResponse + "]",
						true);
				Assert.fail("InCorrect status [" + statusResponse + "]");
			}

			// verify message is null
			String messageResponse = jsonPath.getString("message");
			if (messageResponse == null) {
				Reporter.log(", Correct message [null]", true);
			} else {
				Reporter.log(", InCorrect message [" + messageResponse + "]",
						true);
				Assert.fail("InCorrect message [" + messageResponse + "]");
			}

			try {
				if (!jsonPath.getMap("data.examHierarchyData.course").isEmpty()) {
					// fetch all substream details from db
					ResultSet rs = exceuteDbQuery(
							"select distinct  epm.id as ExamId, bc.base_course_id from exampage_main as epm join exampage_master as epmas on epm.id = epmas.exam_id join examAttributeMapping as eam on epmas.groupId = eam.groupId join exampage_groups as epg on epg.groupId = eam.groupId join base_courses as bc on eam.entityId = bc.base_course_id where bc.is_popular = 1 and eam.entityType ='course' and eam.status = 'live' and epmas.status ='live' and epm.status = 'live' order by epm.id asc;",
							"shiksha");

					// get row count of resultSet of sql query
					int rowcount = getCountOfResultSet(rs);
					//
					if (rowcount > 0) {
						while (rs.next()) {
							int examId = rs.getInt("ExamId");
							int courseId = rs.getInt("base_course_id");
							int groupId = 0;
							ResultSet rsverifyGroup = exceuteDbQuery(
									"select groupId from examAttributeMapping where examId = "
											+ examId
											+ " and entityId = "
											+ courseId
											+ " and entityType = 'course' and status = 'live';",
									"shiksha");
							int rowcountVerifyGroup = getCountOfResultSet(rsverifyGroup);
							if (rowcountVerifyGroup > 0) {
								while (rsverifyGroup.next()) {
									int checkgroupId = rsverifyGroup
											.getInt("groupId");
									if (rowcountVerifyGroup > 1) {
										ResultSet rsfindGroup = exceuteDbQuery(
												"select * from exampage_groups where examId ="
														+ examId
														+ " and groupId ="
														+ checkgroupId
														+ " and status ='live';",
												"shiksha");
										while (rsfindGroup.next()) {
											int isPrimary = rsfindGroup
													.getInt("isPrimary");
											if (isPrimary != 0) {
												groupId = rsfindGroup
														.getInt("groupId");
											}
										}
									} else {
										groupId = rsverifyGroup
												.getInt("groupId");
									}
								}
							}
							if (groupId == 0) {
								ResultSet rsGetGroup = exceuteDbQuery(
										"select groupId from examAttributeMapping where examId = "
												+ examId
												+ " and entityId = "
												+ courseId
												+ " and entityType = 'primaryHierarchy' and status = 'live' order by groupId desc limit 1;",
										"shiksha");
								if (getCountOfResultSet(rsGetGroup) > 0) {
									while (rsGetGroup.next()) {
										groupId = rsGetGroup.getInt("groupId");
									}
								}
							}
							ResultSet rsExamDetails = exceuteDbQuery(
									"select * from exampage_main where id ="
											+ examId + " and status = 'live';",
									"shiksha");

							// Verify Exam name of Popular Course Exam
							if (getCountOfResultSet(rsExamDetails) > 0) {
								while (rsExamDetails.next()) {
									String DBexamName = rsExamDetails
											.getString("name");
									String DBexamFullName = rsExamDetails
											.getString("fullName");
									String DBexamUrl;
									DBexamUrl = rsExamDetails.getString("url");
									if (DBexamUrl == null) {
										DBexamUrl = "";
									}
									ResultSet rsExamYear = exceuteDbQuery(
											"select entityId from examAttributeMapping where examId = "
													+ examId
													+ " and groupId = "
													+ groupId
													+ " and entityType = 'year' and status = 'live';",
											"shiksha");
									int rowCountExamyear = getCountOfResultSet(rsExamYear);
									String DBexamYear = null;

									if (rowCountExamyear > 0) {
										while (rsExamYear.next()) {
											DBexamYear = String
													.valueOf(rsExamYear
															.getInt("entityId"));
										}
									} else {
										DBexamYear = "";
									}
									String DBexamConductBy = "";

									ResultSet rsIsPrimary = exceuteDbQuery(
											"select * from exampage_groups where examId ="
													+ examId + " and groupId ="
													+ groupId
													+ " and status ='live';",
											"shiksha");
									int isPrimary;
									if (getCountOfResultSet(rsIsPrimary) > 0) {
										while (rsIsPrimary.next()) {
											isPrimary = rsIsPrimary
													.getInt("isPrimary");
											if (isPrimary == 0) {
												DBexamUrl = DBexamUrl
														+ "?course=" + groupId;
											}
										}
									}

									String Jsonpopularcourseexamname = jsonPath
											.getString("data.examHierarchyData.course."
													+ courseId
													+ "."
													+ examId
													+ ".examName");
									if (Jsonpopularcourseexamname
											.contains(" & ")) {
										Jsonpopularcourseexamname = Jsonpopularcourseexamname
												.replace(" & ", " and ");
									}
									if (Jsonpopularcourseexamname
											.equalsIgnoreCase(DBexamName)) {
										Reporter.log(
												"Exam name is appear Correct for: "
														+ Jsonpopularcourseexamname,
												true);
										Assert.assertTrue(true,
												"Exam name is appear correct");
									} else {
										Reporter.log(
												"Exam name is not appear Correct for: "
														+ Jsonpopularcourseexamname
														+ " expected Result: "
														+ DBexamName, true);
										filewriter(
												"Exam name is not appear Correct for: "
														+ Jsonpopularcourseexamname
														+ " expected Result: "
														+ DBexamName
														+ " for Base Course "
														+ courseId
														+ " ExamId: " + examId
														+ " groupId: "
														+ groupId,
												"Logs//GetHamburgerData");
										result = true;
									}

									// Verify Exam Full name
									String Jsonpopularcourseexamfullname = jsonPath
											.getString("data.examHierarchyData.course."
													+ courseId
													+ "."
													+ examId
													+ ".examFullName");
									if (Jsonpopularcourseexamfullname == null) {
										Jsonpopularcourseexamfullname = "";
									}
									if (DBexamFullName == null) {
										DBexamFullName = "";
									}
									if (Jsonpopularcourseexamfullname
											.equalsIgnoreCase(DBexamFullName)) {
										Reporter.log(
												"Exam full name is appear Correct for: "
														+ Jsonpopularcourseexamname,
												true);
										Assert.assertTrue(true,
												"Exam full name is appear correct");
									} else {
										Reporter.log(
												"Exam full name is not appear Correct for: "
														+ DBexamFullName
														+ " Actual Result "
														+ Jsonpopularcourseexamname
														+ " expected Result "
														+ DBexamFullName, true);
										filewriter(
												"Exam full name is not appear Correct for: "
														+ DBexamFullName
														+ " Actual Result "
														+ Jsonpopularcourseexamname
														+ " expected Result "
														+ DBexamFullName
														+ " for Base Course "
														+ courseId
														+ " examid: " + examId
														+ " groupId: "
														+ groupId,
												"Logs//GetHamburgerData");
										result = true;
									}

									// Verify exam year
									String Jsonpopularcourseexamyear = jsonPath
											.getString("data.examHierarchyData.course."
													+ courseId
													+ "."
													+ examId
													+ ".examYear");
									if (Jsonpopularcourseexamyear == null) {
										Jsonpopularcourseexamyear = "";
									}
									if (Jsonpopularcourseexamyear
											.equalsIgnoreCase(DBexamYear)) {
										Reporter.log(
												"Exam Year is appear Correct for: "
														+ Jsonpopularcourseexamname,
												true);
										Assert.assertTrue(true,
												"Exam Year is appear correct");
									} else {
										Reporter.log(
												"Exam year is not appear Correct for: "
														+ Jsonpopularcourseexamname
														+ " Actual Result "
														+ Jsonpopularcourseexamyear
														+ " expected Result "
														+ DBexamYear
														+ " for Base Course "
														+ courseId
														+ " for Group Id "
														+ groupId, true);
										filewriter(
												"Exam year is not appear Correct for: "
														+ Jsonpopularcourseexamname
														+ " Actual Result "
														+ Jsonpopularcourseexamyear
														+ " expected Result "
														+ DBexamYear
														+ " for Base Course "
														+ courseId
														+ " examid: " + examId
														+ " groupId: "
														+ groupId,
												"Logs//GetHamburgerData");
										result = true;
									}

									// verify conducted by
									String Jsonpopularcourseexamconductby = jsonPath
											.getString("data.examHierarchyData.course."
													+ courseId
													+ "."
													+ examId
													+ ".conductedBy");
									if (Jsonpopularcourseexamconductby == null) {
										Jsonpopularcourseexamconductby = "";
									}
									if (Jsonpopularcourseexamconductby
											.equalsIgnoreCase(DBexamConductBy)) {
										Reporter.log(
												"Exam ConductedBy is appear Correct for: "
														+ Jsonpopularcourseexamname,
												true);
										Assert.assertTrue(true,
												"Exam ConductedBy is appear correct");
									} else {
										Reporter.log(
												"Exam ConductedBy is not appear Correct for: "
														+ Jsonpopularcourseexamname
														+ " Actual Result "
														+ Jsonpopularcourseexamname
														+ " expected Result "
														+ DBexamConductBy, true);
										filewriter(
												"Exam ConductedBy is not appear Correct for: "
														+ Jsonpopularcourseexamname
														+ " Actual Result "
														+ Jsonpopularcourseexamname
														+ " expected Result "
														+ DBexamConductBy
														+ " for Base Course "
														+ courseId
														+ " examid: " + examId
														+ " groupId: "
														+ groupId,
												"Logs//GetHamburgerData");
										result = true;
									}
									// Verify url
									String Jsonpopularcourseurl;

									try {
										Jsonpopularcourseurl = jsonPath
												.getString("data.examHierarchyData.course."
														+ courseId
														+ "."
														+ examId + ".url");
									} catch (NullPointerException e) {
										Jsonpopularcourseurl = "";
									}
									if (DBexamUrl.contains("and")) {
										DBexamUrl = DBexamUrl.replace("and-",
												"");
									}
									Jsonpopularcourseurl = Jsonpopularcourseurl
											.substring(Jsonpopularcourseurl
													.indexOf(".com") + 4);
									if (Jsonpopularcourseurl
											.equalsIgnoreCase(DBexamUrl)) {
										Reporter.log(
												"Exam Url is appear Correct for: "
														+ Jsonpopularcourseexamname,
												true);
										Assert.assertTrue(true,
												"Exam Url is appear correct");
									} else {
										Reporter.log(
												"Exam Url is not appear Correct for: "
														+ Jsonpopularcourseexamname
														+ " Actual Result "
														+ Jsonpopularcourseurl
														+ " expected Result "
														+ DBexamUrl, true);
										filewriter(
												"Exam Url is not appear Correct for: "
														+ Jsonpopularcourseexamname
														+ " Actual Result "
														+ Jsonpopularcourseurl
														+ " expected Result "
														+ DBexamUrl
														+ " for Base Course "
														+ courseId
														+ " examid: " + examId
														+ " groupId: "
														+ groupId,
												"Logs//GetHamburgerData");
//										result = true;
									}
								}
							}
						}
					}
					// Assert.assertTrue(!result,"Popular Course Exam List Result is not appear correct please check logs");
					Reporter.log("Popular Course Map run Successfully", true);
				} else {
					Reporter.log(
							"Exam for popular Courses is empty"
									+ jsonPath
											.getList("data.examHierarchyData.course"),
							true);
				}
			} catch (NullPointerException e) {
				Reporter.log(
						"Exam for popular Courses is empty"
								+ jsonPath
										.getList("data.examHierarchyData.course"),
						true);
			}
			try {
				if (!jsonPath.getMap("data.examHierarchyData.hierarchy")
						.isEmpty()) {

					ResultSet rs = exceuteDbQuery(
							"select distinct  epm.id as ExamId, bh.hierarchy_id, bh.stream_id, epm.name,epm.fullName, epm.conductedBy, epm.url from exampage_main as epm join exampage_master as epmas on epm.id = epmas.exam_id join examAttributeMapping as eam on epmas.groupId = eam.groupId join exampage_groups as epg on epg.groupId = eam.groupId join base_hierarchies as bh on eam.entityId = bh.hierarchy_id where eam.entityType ='primaryHierarchy' and eam.status = 'live' and epmas.status ='live' and epm.status = 'live' and bh.status ='live' order by epm.id asc;",
							"shiksha");

					// get row count of resultSet of sql query
					int rowcount = getCountOfResultSet(rs);
					if (rowcount > 0) {
						while (rs.next()) {
							int examId = rs.getInt("ExamId");
							int hierarchyId = rs.getInt("hierarchy_id");
							int streamId = rs.getInt("stream_id");
							String dbExamName = rs.getString("name");
							String dbExamFullname = rs.getString("fullName");
							String dbExamUrl = rs.getString("url");
							String dbConductBy = "";
							int groupId = 0;
							int dbisPrimary = 0;
							int counter;
							String dbExamYear = "";
							if (streamId == hierarchyId) {
								counter = 1;
							} else {
								counter = 2;
							}
							if (dbExamUrl == null) {
								dbExamUrl = "";
							}
							ResultSet rsverifyGroup = exceuteDbQuery(
									"select groupId from examAttributeMapping where examId = "
											+ examId
											+ " and entityId = "
											+ hierarchyId
											+ " and entityType = 'primaryHierarchy' and status = 'live';",
									"shiksha");
							int rowcountVerifyGroup = getCountOfResultSet(rsverifyGroup);
							if (rowcountVerifyGroup > 0) {
								while (rsverifyGroup.next()) {
									int checkgroupId = rsverifyGroup
											.getInt("groupId");
									if (rowcountVerifyGroup > 1) {
										ResultSet rsfindGroup = exceuteDbQuery(
												"select * from exampage_groups where examId ="
														+ examId
														+ " and groupId ="
														+ checkgroupId
														+ " and status ='live';",
												"shiksha");
										while (rsfindGroup.next()) {
											int isPrimary = rsfindGroup
													.getInt("isPrimary");
											if (isPrimary != 0) {
												groupId = rsfindGroup
														.getInt("groupId");
											}
										}
									} else {
										groupId = rsverifyGroup
												.getInt("groupId");
									}
								}
							}
							if (groupId == 0) {
								ResultSet rsGetGroup = exceuteDbQuery(
										"select groupId from examAttributeMapping where examId = "
												+ examId
												+ " and entityId = "
												+ hierarchyId
												+ " and entityType = 'primaryHierarchy' and status = 'live' order by groupId desc limit 1;",
										"shiksha");
								if (getCountOfResultSet(rsGetGroup) > 0) {
									while (rsGetGroup.next()) {
										groupId = rsGetGroup.getInt("groupId");
									}
								}
							}
							ResultSet rsGetExamYear = exceuteDbQuery(
									"select * From examAttributeMapping where examId = "
											+ examId
											+ " and groupId= "
											+ groupId
											+ " and entityType = 'year' and status = 'live';",
									"shiksha");
							if (getCountOfResultSet(rsGetExamYear) > 0) {
								while (rsGetExamYear.next()) {
									dbExamYear = String.valueOf(rsGetExamYear
											.getInt("entityId"));
								}
							}

							ResultSet rsGetIsPrimary = exceuteDbQuery(
									"select * From exampage_groups where examId = "
											+ examId + " and groupId = "
											+ groupId + " and status ='live';",
									"shiksha");
							if (getCountOfResultSet(rsGetIsPrimary) > 0) {
								while (rsGetIsPrimary.next()) {
									dbisPrimary = rsGetIsPrimary
											.getInt("isPrimary");
								}
							}
							if (dbisPrimary == 0) {
								dbExamUrl = dbExamUrl + "?course=" + groupId;
							}
							for (; counter > 0; counter--) {
								if (counter == 1) {
									hierarchyId = streamId;
								}
								String Jsonhierarchyexamname = jsonPath
										.getString("data.examHierarchyData.hierarchy."
												+ hierarchyId
												+ "."
												+ examId
												+ ".examName");

								if (Jsonhierarchyexamname
										.equalsIgnoreCase(dbExamName)) {
									Reporter.log(
											"Exam name is appear Correct for: "
													+ Jsonhierarchyexamname,
											true);
									Assert.assertTrue(true,
											"Exam name is appear correct");
								} else {
									Reporter.log(
											"Exam name is not appear Correct for: "
													+ Jsonhierarchyexamname
													+ " expected Result: "
													+ dbExamName, true);
									filewriter(
											"Exam name is not appear Correct for: "
													+ Jsonhierarchyexamname
													+ " expected Result: "
													+ dbExamName
													+ " for hierarchyId "
													+ hierarchyId + " examid: "
													+ examId + " groupId: "
													+ groupId,
											"Logs//GetHamburgerData");
									result = true;
								}

								// Verify Exam Full name
								String Jsonhierarchyexamfullname = jsonPath
										.getString("data.examHierarchyData.hierarchy."
												+ hierarchyId
												+ "."
												+ examId
												+ ".examFullName");
								if (Jsonhierarchyexamfullname == null) {
									Jsonhierarchyexamfullname = "";
								}
								if (dbExamFullname == null) {
									dbExamFullname = "";
								}
								if (Jsonhierarchyexamfullname
										.equalsIgnoreCase(dbExamFullname)) {
									Reporter.log(
											"Exam full name is appear Correct for: "
													+ Jsonhierarchyexamname,
											true);
									Assert.assertTrue(true,
											"Exam full name is appear correct");
								} else {
									Reporter.log(
											"Exam full name is not appear Correct for: "
													+ dbExamFullname
													+ " Actual Result "
													+ Jsonhierarchyexamname
													+ " expected Result "
													+ dbExamFullname, true);
									filewriter(
											"Exam full name is not appear Correct for: "
													+ dbExamFullname
													+ " Actual Result "
													+ Jsonhierarchyexamname
													+ " expected Result "
													+ dbExamFullname
													+ " for hierarchyId "
													+ hierarchyId + " examid: "
													+ examId + " groupId: "
													+ groupId,
											"Logs//GetHamburgerData");
									result = true;
								}

								// Verify exam year
								String Jsonhierarchyexamyear = jsonPath
										.getString("data.examHierarchyData.hierarchy."
												+ hierarchyId
												+ "."
												+ examId
												+ ".examYear");
								if (Jsonhierarchyexamyear == null) {
									Jsonhierarchyexamyear = "";
								}
								if (Jsonhierarchyexamyear
										.equalsIgnoreCase(dbExamYear)) {
									Reporter.log(
											"Exam Year is appear Correct for: "
													+ Jsonhierarchyexamname,
											true);
									Assert.assertTrue(true,
											"Exam Year is appear correct");
								} else {
									Reporter.log(
											"Exam year is not appear Correct for: "
													+ Jsonhierarchyexamname
													+ " Actual Result: "
													+ Jsonhierarchyexamyear
													+ " expected Result: "
													+ dbExamYear
													+ " for hierarchyId: "
													+ hierarchyId
													+ " for Group Id: "
													+ groupId, true);
									filewriter(
											"Exam year is not appear Correct for: "
													+ Jsonhierarchyexamname
													+ " Actual Result: "
													+ Jsonhierarchyexamyear
													+ " expected Result: "
													+ dbExamYear
													+ " for hierarchyId "
													+ hierarchyId + " examid: "
													+ examId + " groupId: "
													+ groupId,
											"Logs//GetHamburgerData");
									result = true;
								}

								// verify conducted by
								String Jsonhierarchyexamconductby = jsonPath
										.getString("data.examHierarchyData.hierarchy."
												+ hierarchyId
												+ "."
												+ examId
												+ ".conductedBy");
								if (Jsonhierarchyexamconductby == null) {
									Jsonhierarchyexamconductby = "";
								}
								if (Jsonhierarchyexamconductby
										.equalsIgnoreCase(dbConductBy)) {
									Reporter.log(
											"Exam ConductedBy is appear Correct for: "
													+ Jsonhierarchyexamname,
											true);
									Assert.assertTrue(true,
											"Exam ConductedBy is appear correct");
								} else {
									Reporter.log(
											"Exam ConductedBy is not appear Correct for: "
													+ Jsonhierarchyexamname
													+ " Actual Result "
													+ Jsonhierarchyexamname
													+ " expected Result "
													+ dbConductBy, true);
									filewriter(
											"Exam ConductedBy is not appear Correct for: "
													+ Jsonhierarchyexamname
													+ " Actual Result "
													+ Jsonhierarchyexamname
													+ " expected Result "
													+ dbConductBy
													+ " for hierarchyId "
													+ hierarchyId + " examid: "
													+ examId + " groupId: "
													+ groupId,
											"Logs//GetHamburgerData");
									result = true;
								}
								// Verify url
								String Jsonhierarchyurl;

								try {
									Jsonhierarchyurl = jsonPath
											.getString("data.examHierarchyData.hierarchy."
													+ hierarchyId
													+ "."
													+ examId + ".url");
								} catch (NullPointerException e) {
									Jsonhierarchyurl = "";
								}

								Jsonhierarchyurl = Jsonhierarchyurl
										.substring(Jsonhierarchyurl
												.indexOf(".com") + 4);
								if (Jsonhierarchyurl
										.equalsIgnoreCase(dbExamUrl)) {
									Reporter.log(
											"Exam Url is appear Correct for: "
													+ Jsonhierarchyexamname,
											true);
									Assert.assertTrue(true,
											"Exam Url is appear correct");
								} else {
									Reporter.log(
											"Exam Url is not appear Correct for: "
													+ Jsonhierarchyexamname
													+ " Actual Result "
													+ Jsonhierarchyurl
													+ " expected Result "
													+ dbExamUrl, true);
									filewriter(
											"Exam Url is not appear Correct for: "
													+ Jsonhierarchyexamname
													+ " Actual Result "
													+ Jsonhierarchyurl
													+ " expected Result "
													+ dbExamUrl
													+ " for hierarchyId "
													+ hierarchyId + " examid: "
													+ examId + " groupId: "
													+ groupId,
											"Logs//GetHamburgerData");
//									result = true;
								}
							}
						}
					}
					// Assert.assertTrue(!result,
					// "Hierachies Exam list Failed Please Check Logs");
					Reporter.log("Hierachies Map Run Successfully", true);
				} else {
					Reporter.log(
							"Exam for hierarchy Courses is empty"
									+ jsonPath
											.getList("data.examHierarchyData.hierarchy"),
							true);
				}
			} catch (NullPointerException e) {
				Reporter.log(
						"Exam for hierarchy Courses is empty"
								+ jsonPath
										.getList("data.examHierarchyData.hierarchy"),
						true);
			}
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("SQLException occured :" + e.getMessage());
		}
		return result;
	}

	public boolean verifyRankingStreamWise(JsonPath jsonPath) {
		filewriter("verifyRankingStreamWise", "Logs//GetHamburgerData");
		result = false;
		try {
			HashMap<String, String> innerMap;
			ArrayList<HashMap<String, String>> dbRankingList = new ArrayList<HashMap<String, String>>();
			ResultSet rs = exceuteDbQuery(
					"select * from streams where status = 'live';", "shiksha");
			if (getCountOfResultSet(rs) > 0) {
				while (rs.next()) {
					dbRankingList.clear();
					int streamId = rs.getInt("stream_id");
					ResultSet rsRankingPage = exceuteDbQuery(
							"select * from ranking_pages where stream_id = "
									+ streamId + " and status ='live';",
							"shiksha");
					if (getCountOfResultSet(rsRankingPage) > 0) {
						while (rsRankingPage.next()) {
							String pageKey = rsRankingPage.getInt("id")
									+ "-2-0-0-0";
							int baseCourseId = rsRankingPage
									.getInt("base_course_id");
							int substreamId = rsRankingPage
									.getInt("substream_id");
							String rankingpagename = rsRankingPage
									.getString("ranking_page_text");

							innerMap = new HashMap<String, String>();
							innerMap.put("defaultPublisherId", String
									.valueOf(rsRankingPage
											.getInt("default_publisher")));
							innerMap.put("rankingPageId",
									String.valueOf(rsRankingPage.getInt("id")));
							innerMap.put("rankingPageName", rankingpagename);
							innerMap.put("tupleType",
									rsRankingPage.getString("tuple_type"));
							innerMap.put("disclaimer",
									rsRankingPage.getString("disclaimer"));
							innerMap.put("streamId", String
									.valueOf(rsRankingPage.getInt("stream_id")));
							innerMap.put("substreamId",
									String.valueOf(substreamId));
							innerMap.put("specializationId", String
									.valueOf(rsRankingPage
											.getInt("specialization_id")));
							innerMap.put("educationType", String
									.valueOf(rsRankingPage
											.getInt("education_type")));
							innerMap.put("deliveryMethod", String
									.valueOf(rsRankingPage
											.getInt("delivery_method")));
							innerMap.put("credential",
									String.valueOf(rsRankingPage
											.getInt("credential")));
							innerMap.put("baseCourseId",
									String.valueOf(baseCourseId));
							innerMap.put("countryId", "2");
							innerMap.put("stateId", "0");
							innerMap.put("stateName", "null");
							innerMap.put("cityId", "0");
							innerMap.put("cityName", "null");
							innerMap.put("examId", "0");
							innerMap.put("examName", "null");
							innerMap.put("pageKey", pageKey);
							innerMap.put("url", pageKey);
							dbRankingList.add(innerMap);
						}
						try {
							if (!jsonPath.getList(
									"data.rankingStreamWiseData." + streamId)
									.isEmpty()) {
								List<HashMap<String, String>> rankjsonList = new ArrayList<HashMap<String, String>>(
										jsonPath.getList("data.rankingStreamWiseData."
												+ streamId));
								List<HashMap<String, String>> jsonList = new ArrayList<HashMap<String, String>>();
								for (int i = 0; i < rankjsonList.size(); i++) {
									HashMap<String, String> hashmap = rankjsonList
											.get(i);
									String url = hashmap.get("url");
									url = url
											.substring(url.indexOf("india/") + 6);
									hashmap.put("url", url);
									jsonList.add(hashmap);
								}
								for (int j = 0; j < jsonList.size(); j++) {
									HashMap<String, String> jsonTempMap = jsonList
											.get(j);
									HashMap<String, String> dbTempMap = dbRankingList
											.get(j);

									for (Map.Entry<String, String> jsonmap : jsonTempMap
											.entrySet()) {
										String jsonkey = jsonmap.getKey();
										String jsonvalue = String
												.valueOf(jsonmap.getValue());
										String dbvalue = dbTempMap.get(jsonkey);

										if (jsonvalue.equals(dbvalue)) {
											Reporter.log(
													"RankingStreamWise Matches Successfully for key: "
															+ jsonkey
															+ " StreamId: "
															+ streamId, true);
											 Assert.assertTrue(true);
										} else {
											Reporter.log(
													"RankingStreamWise Matches Failed for key: "
															+ jsonkey
															+ " StreamId: "
															+ streamId, true);
											filewriter(
													"Actual Result: JsonKey: "
															+ jsonkey
															+ " JsonValue: "
															+ jsonvalue,
													"Logs//GetHamburgerData");
											filewriter(
													"expected Result: dbKey: "
															+ jsonkey
															+ " dbValue: "
															+ dbvalue,
													"Logs//GetHamburgerData");
											filewriter("Actual Result: "
													+ jsonList,
													"Logs//GetHamburgerData");
											filewriter("expected Result: "
													+ dbRankingList,
													"Logs//GetHamburgerData");
											result = true;
										}
									}
								}
							}
						}

						catch (NullPointerException e) {
							Reporter.log("list Not found for Stream Id: "
									+ streamId, true);
						}
					}
				}
			}
		} catch (SQLException e) {
		}
		return result;
	}
	
	public boolean verifycampusRepProgramDetails(JsonPath jsonPath){
		filewriter("verifycampusRepProgramDetails", "Logs//GetHamburgerData");
		result = false;
		try {
			HashMap<String, String> innerMap;
			ArrayList<HashMap<String, String>> campusRapDBList = new ArrayList<HashMap<String, String>>();
			ResultSet rs = exceuteDbQuery(
					"select * from campusConnectProgram where status ='live';",
					"shiksha");
			if (getCountOfResultSet(rs) > 0) {
				while (rs.next()) {
					int entityId = rs.getInt("entityId");
					String entityType = rs.getString("entityType");
					int programId= rs.getInt("programId");
					String url = null;
					if(entityType.equals("stream")){
						ResultSet rsStreamName = exceuteDbQuery("select * from streams where stream_id="+entityId+" and status='live';", "shiksha");
						if(getCountOfResultSet(rsStreamName)>0){
							while(rsStreamName.next()){
								String streamName = rsStreamName.getString("name");
							if(streamName.contains(" & ")){
								streamName =streamName.replace(" & ", " ");
							}
							streamName =streamName.toLowerCase().trim();
							streamName = streamName.replace(" ", "-");
							url = "/"+streamName+"/resources/campus-connect-program-"+programId;
							}
						}
						
					}
					else if(entityType.equals("baseCourse")||entityType.equals("generic")){
						ResultSet rsStreamName = exceuteDbQuery("select * from base_courses where base_course_id ="+entityId+" and status='live';", "shiksha");
						if(getCountOfResultSet(rsStreamName)>0){
							while(rsStreamName.next()){
								String streamName = rsStreamName.getString("alias");
								if(streamName.equals("")||streamName.equals("null")){
									streamName = rsStreamName.getString("name");
								}
							if(streamName.contains(".")){
								streamName =streamName.replace(".", " ");
							}
							streamName = streamName.trim().toLowerCase();
							streamName = streamName.replace(" ", "-");
							url = "/"+streamName+"/resources/campus-connect-program-"+programId;
							}
						}
					}
					else if(entityType.equals("substream")){
						ResultSet rsStreamName = exceuteDbQuery(
								"select str.name as streamName, str.alias as alias, ss.name as substreamName from streams as str join substreams as ss on str.stream_id = ss.primary_stream_id where ss.substream_id = "+entityId+" and ss.status = 'live' and str.status='live'; ",
								"shiksha");
						if(getCountOfResultSet(rsStreamName)>0){
							while(rsStreamName.next()){
								String streamName = rsStreamName.getString("alias");
								String subStreamName = rsStreamName.getString("substreamName");
								if(streamName.equals("")||streamName.equals(null)){
									streamName = rsStreamName.getString("streamName");
								}
							if(streamName.contains(" & ")){
								streamName =streamName.replace(" & ", " ");
							}
							if(subStreamName.contains(" / ")){
								subStreamName =subStreamName.replace(" / ", " ");
							}
							streamName = streamName.trim().toLowerCase();
							subStreamName = subStreamName.trim().toLowerCase();
							streamName = streamName.replace(" ", "-");
							subStreamName = subStreamName.replace(" ", "-");
							url = "/"+streamName+"/"+subStreamName+"/resources/campus-connect-program-"+programId;
							}
						}
					}
					innerMap = new HashMap<String, String>();
					innerMap.put("programId",
							String.valueOf(programId));
					innerMap.put("programName", rs.getString("programName"));
					innerMap.put("entityId",
							String.valueOf(entityId));
					innerMap.put("entityType",entityType );
					innerMap.put("ccUrl", url);
					campusRapDBList.add(innerMap);
				}
				try {
					if (!jsonPath.getList("data.campusRepProgramDetails")
							.isEmpty()) {
						List<HashMap<String, String>> campusRapJsonList = new ArrayList<HashMap<String, String>>(
								jsonPath.getList("data.campusRepProgramDetails"));
						List<HashMap<String, String>> jsonList = new ArrayList<HashMap<String, String>>();
						for (int i = 0; i < campusRapJsonList.size(); i++) {
							HashMap<String, String> hashmap = campusRapJsonList
									.get(i);
							String ccUrl = hashmap.get("ccUrl");
							ccUrl = ccUrl
									.substring(ccUrl.indexOf(".com") + 4);
							hashmap.put("ccUrl", ccUrl);
							jsonList.add(hashmap);
						}
						for (int j = 0; j < jsonList.size(); j++) {
							HashMap<String, String> jsonTempMap = jsonList
									.get(j);
							HashMap<String, String> dbTempMap = campusRapDBList
									.get(j);

							for (Map.Entry<String, String> jsonmap : jsonTempMap
									.entrySet()) {
								String jsonkey = jsonmap.getKey();
								String jsonvalue = String.valueOf(jsonmap
										.getValue());
								String dbvalue = dbTempMap.get(jsonkey);

								if (jsonvalue.equals(dbvalue)) {
									Reporter.log(
											"campusRepProgramDetails Matches Successfully for key: "
													+ jsonkey, true);
									 Assert.assertTrue(true);
								} else {
									Reporter.log(
											"campusRepProgramDetails Matches Failed for key: "
													+ jsonkey, true);
									filewriter("Actual Result: JsonKey: "
											+ jsonkey + " JsonValue: "
											+ jsonvalue,
											"Logs//GetHamburgerData");
									filewriter("expected Result: dbKey: "
											+ jsonkey + " dbValue: " + dbvalue,
											"Logs//GetHamburgerData");
									filewriter("Actual Result: " + jsonList,
											"Logs//GetHamburgerData");
									filewriter("expected Result: "
											+ campusRapDBList,
											"Logs//GetHamburgerData");
									result = true;
								}
							}
						}
					}
				}

				catch (NullPointerException e) {
					Reporter.log("list Not found for campusRepProgramDetails"
							+ jsonPath.getList("data.campusRepProgramDetails"),
							true);
				}
			} 

		} catch (SQLException e) {
		}
		return result;
	}
	
	@Test(priority = 2)
	public static void respnseTimeStats_getHamburgerData() {
		respnseTimeStatsCommon(responseTimes,
				apiPath.substring(apiPath.lastIndexOf("/") + 1));
	}
}
