package common;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class DatabaseCommon extends Common {
	static ResultSet rs;

	public static ResultSet baseAttributeList(String value) {
		rs = exceuteDbQuery(
				"select * From base_attribute_list where value_id =" + value
						+ " and status = 'live';", "shiksha");
		return rs;
	}

	public static ResultSet getHighlights(String courseId) {
		rs = exceuteDbQuery(
				"select description, info_type from shiksha_courses_additional_info where course_id = "
						+ courseId
						+ " and info_type ='usp' and status ='live' order by id asc;",
				"shiksha");
		return rs;
	}

	public static ResultSet getCourseStructure(String courseId) {
		rs = exceuteDbQuery(
				"select period,period_value, courses_offered from shiksha_courses_structure_offered_courses where course_id = "
						+ courseId + " and status = 'live'", "shiksha");
		return rs;
	}

	public static ResultSet getParentInstituteId(String courseId) {
		rs = exceuteDbQuery(
				"select parent_id, parent_type, total_seats from shiksha_courses where course_id ="
						+ courseId + " and status ='live'", "shiksha");
		return rs;
	}

	public static ResultSet getListingLocationId(String courseId,
			String cityId, String localityId) {
		rs = exceuteDbQuery(
				"select sil.listing_location_id, sil.state_id, sil.city_id,sil.locality_id, scl.is_main from shiksha_institutes_locations as sil join shiksha_courses_locations as scl on sil.listing_location_id = scl.listing_location_id where scl.course_id = "
						+ courseId
						+ " and sil.city_id = '"
						+ cityId
						+ "' and sil.status = 'live' and scl.status = 'live';",
				"shiksha");
		if (getCountOfResultSet(rs) == 0) {
			rs = exceuteDbQuery(
					"select sil.listing_location_id, sil.state_id, sil.city_id,sil.locality_id, scl.is_main from shiksha_institutes_locations as sil join shiksha_courses_locations as scl on sil.listing_location_id = scl.listing_location_id where scl.course_id ="
							+ courseId
							+ " and scl.is_main = 1 and sil.status = 'live' and scl.status = 'live';",
					"shiksha");
		}
		return rs;
	}

	public static String getCityName(String cityId) {
		String CityName = null;
		try {
			ResultSet rs = exceuteDbQuery(
					"select city_name from countryCityTable where city_id = "
							+ cityId + ";", "shiksha");
			if (getCountOfResultSet(rs) > 0) {
				while (rs.next()) {
					CityName = rs.getString("city_name");
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return CityName;
	}

	public static String getlocalityName(String localityId) {
		String localityName = null;
		try {
			ResultSet rs = exceuteDbQuery(
					"select localityName from localityCityMapping where localityId = "
							+ localityId + " and status ='live';", "shiksha");
			if (getCountOfResultSet(rs) > 0) {
				while (rs.next()) {
					localityName = rs.getString("localityName");
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return localityName;
	}

	public static String getStateName(String stateId) {
		String StateName = null;
		try {
			ResultSet rs = exceuteDbQuery(
					"select state_name from stateTable where state_id =  "
							+ stateId + ";", "shiksha");
			if (getCountOfResultSet(rs) > 0) {
				while (rs.next()) {
					StateName = rs.getString("state_name");
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return StateName;
	}

	public static ResultSet getFeesOnLocation(String courseId,
			String listingLocationId) {
		rs = exceuteDbQuery(
				"select scf.category, scf.period, scf.fees_value, scf.fees_unit, scf.fees_type, scf.total_includes, scf.fees_disclaimer, cur.currency_code as fees_unit_name from currency as cur join shiksha_courses_fees as scf on cur.id =scf.fees_unit where course_id ="
						+ courseId
						+ " and listing_location_id ="
						+ listingLocationId + " and status = 'live';",
				"shiksha");
		return rs;
	}

	public static ResultSet getRankingBySources(String courseId) {
		rs = exceuteDbQuery(
				"select distinct rpcsd.source_id, rps.publisher_id, rps.publisher_name, rpd.ranking_page_id, "
						+ "rpcsd.rank, rpd.institute_id, rpd.course_id, rps.year from ranking_page_sources as rps "
						+ "join ranking_page_source_mapping as rpsm on rps.source_id = rpsm.source_id "
						+ "join ranking_page_course_source_data as rpcsd on rpsm.source_id = rpcsd.source_id "
						+ "join ranking_page_data as rpd on rpcsd.ranking_page_course_id = rpd.id "
						+ "join ranking_pages as rp on rp.id=rpd.ranking_page_id where rpd.course_id = "
						+ courseId
						+ " and rpsm.ranking_page_id =rp.id and  rp.status = 'live' and rps.status = 'live' and"
						+ " rpsm.status='live' order by rps.year desc, rpcsd.rank asc; ",
				"shiksha");
		return rs;
	}

	public static ResultSet getVirtualCityId(String cityId) {
		rs = exceuteDbQuery("select * from virtualCityMapping where city_id ="
				+ cityId + " and virtualCityId !=" + cityId + ";", "shiksha");
		return rs;
	}

	public static ResultSet getRankingPageDetails(String rankingPageid) {
		rs = exceuteDbQuery("select * from ranking_pages where id = "
				+ rankingPageid + " and status = 'live';", "shiksha");
		return rs;
	}

	public static String getBaseCourseName(String BaseCourseId) {
		String baseCourseName = null;
		try {
			rs = exceuteDbQuery(
					"select name from base_courses where base_course_id ="
							+ BaseCourseId + " and status = 'live';", "shiksha");
			if (getCountOfResultSet(rs) > 0) {
				while (rs.next()) {
					baseCourseName = rs.getString("name");
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return baseCourseName;
	}

	public static String getSpecializationName(String spId) {
		String SpName = null;
		try {
			rs = exceuteDbQuery(
					"select name from base_courses where base_course_id ="
							+ spId + " and status = 'live';", "shiksha");
			if (getCountOfResultSet(rs) > 0) {
				while (rs.next()) {
					SpName = rs.getString("name");
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return SpName;
	}

	public static ResultSet getEligibilitymain(String courseId) {
		rs = exceuteDbQuery(
				"select  batch_year, `work-ex_max`, `work-ex_min`, age_max, age_min,"
						+ " international_students_desc, description, subjects from shiksha_courses_eligibility_main "
						+ "where course_id = " + courseId
						+ " and status = 'live'", "shiksha");
		return rs;
	}

	public static String getCourseEligibilitySpecRequirement(String courseId,
			String standard) {
		String DBSpecRequirement = null;
		try {
			rs = exceuteDbQuery(
					"select specific_requirement from shiksha_courses_eligibility_score where course_id = "
							+ courseId
							+ " and standard = '"
							+ standard
							+ "' and status = 'live' and category is null;",
					"shiksha");
			if (getCountOfResultSet(rs) > 0) {
				while (rs.next()) {
					DBSpecRequirement = rs.getString("specific_requirement");
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return DBSpecRequirement;
	}

	public static ResultSet getEligibilityScoreData(String courseId,
			String standard, String category) {

		// String DBcategory = rs.getString("category");
		// String DBunit = rs.getString("unit");
		// String DBvalue = rs.getString("value");
		// String DBmaxValue = rs.getString("max_value");

		rs = exceuteDbQuery(
				"select * from shiksha_courses_eligibility_score where course_id = "
						+ courseId + " and status = 'live' and standard = '"
						+ standard + "' and category = '" + category + "';",
				"shiksha");
		if (!category.equals("general") && getCountOfResultSet(rs) == 0) {
			category = "general";
		}
		rs = exceuteDbQuery(
				"select * from shiksha_courses_eligibility_score where course_id = "
						+ courseId + " and status = 'live' and standard = '"
						+ standard + "' and category = '" + category + "';",
				"shiksha");
		return rs;
	}

	public static ResultSet getEligibilityCutOffDataforQuota(String courseId,
			String category) {
		rs = exceuteDbQuery(
				"select quota,category ,cut_off_value from shiksha_courses_exams_cut_off where course_id = "
						+ courseId
						+ " and category = '"
						+ category
						+ "' and exam_id is null and status ='live';",
				"shiksha");
		if (!category.equals("general") && getCountOfResultSet(rs) == 0) {
			category = "general";
		}
		rs = exceuteDbQuery(
				"select quota,category ,cut_off_value from shiksha_courses_exams_cut_off where course_id = "
						+ courseId
						+ " and category = '"
						+ category
						+ "' and exam_id is null and status ='live';",
				"shiksha");

		return rs;
	}

	public static ResultSet getEligibilityCutOffDataforExam(String courseId,
			String category) {
		rs = exceuteDbQuery(
				"select exam_id, exam_name, category, `value`, unit, max_value from shiksha_courses_eligibility_exam_score where course_id = "
						+ courseId
						+ " and category = '"
						+ category
						+ "' and status ='live';", "shiksha");
		if (!category.equals("general") && getCountOfResultSet(rs) == 0) {
			category = "general";
		}
		rs = exceuteDbQuery(
				"select exam_id, exam_name, category, `value`, unit, max_value from shiksha_courses_eligibility_exam_score where course_id = "
						+ courseId
						+ " and category = '"
						+ category
						+ "' and status ='live';", "shiksha");

		return rs;
	}

	public static ResultSet getEligibilityCourseExams(String courseId) {
		rs = exceuteDbQuery(
				"select exam_id, exam_name, category, `value`, unit, max_value from shiksha_courses_eligibility_exam_score where course_id = "
						+ courseId + " and status ='live';", "shiksha");

		return rs;
	}

	public static Map<String, String> getEligibiltyCategoryList(String courseId) {
		Map<String, String> categoryList = new HashMap<String, String>();

		try {
			rs = exceuteDbQuery(
					"select distinct category from shiksha_courses_eligibility_score where course_id = "
							+ courseId
							+ " and category is not null and status = 'live';",
					"shiksha");
			if (getCountOfResultSet(rs) > 0) {
				while (rs.next()) {
					String catString = rs.getString("category");
					String catKeyString = catString.replace("_", " - ")
							.toUpperCase();
					categoryList.put(catKeyString, catString);
				}
			}
			rs = exceuteDbQuery(
					"select distinct category from shiksha_courses_eligibility_exam_score where course_id = "
							+ courseId + " and status ='live' ;", "shiksha");
			if (getCountOfResultSet(rs) > 0) {
				while (rs.next()) {
					String catString = rs.getString("category");
					if (catString != null) {
						categoryList.put(catString.replace("_", " - ")
								.toUpperCase(), catString);
					}
				}
			}
			
			rs= exceuteDbQuery("select distinct category from shiksha_courses_exams_cut_off where course_id="+courseId+" and status = 'live';", "shiksha");
			if (getCountOfResultSet(rs) > 0) {
				while (rs.next()) {
					String catString = rs.getString("category");
					if (catString != null) {
						categoryList.put(catString.replace("_", " - ")
								.toUpperCase(), catString);
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return categoryList;
	}

	public static String getExamUrl(String examId) {
		String examUrl = null;
		try {
			rs = exceuteDbQuery("select url from exampage_main where id="
					+ examId + " and status ='live';", "shiksha");
			if (getCountOfResultSet(rs) > 0) {
				while (rs.next()) {
					examUrl = rs.getString("url");
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return examUrl;
	}

	public static String getEligibilityExamRelatedState(String courseId,
			String examId) {
		String DBrelatedState = null;
		try {
			rs = exceuteDbQuery(
					"select quota from shiksha_courses_exams_cut_off where course_id = "
							+ courseId
							+ " and exam_id="
							+ examId
							+ " and status = 'live' and quota like 'related%' limit 1;",
					"shiksha");
			if (getCountOfResultSet(rs) > 0) {
				while (rs.next()) {
					DBrelatedState = rs.getString("quota");
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return DBrelatedState;
	}

	public static ResultSet checkEligibilityExamCutoff(String courseId,
			String category) {
		rs = exceuteDbQuery(
				"select * from shiksha_courses_exams_cut_off where course_id = "
						+ courseId
						+ " and category = '"
						+ category
						+ "' and cut_off_type = 'exam' and quota not like 'relate%' and status = 'live';",
				"shiksha");
		if (getCountOfResultSet(rs) == 0) {
			category = "general";
			rs = exceuteDbQuery(
					"select * from shiksha_courses_exams_cut_off where course_id = "
							+ courseId
							+ " and category = '"
							+ category
							+ "' and quota not like 'relate%' and cut_off_type = 'exam' and status = 'live';",
					"shiksha");
		}

		return rs;
	}

	public static ResultSet getCourseFee(String courseId) {
		rs = exceuteDbQuery(
				"select * from shiksha_courses_fees where course_id = "
						+ courseId
						+ " and listing_location_id = -1 and fees_unit is not null and status ='live';",
				"shiksha");

		return rs;
	}

	public static String getCurrencyName(String currId) {
		String currName = null;
		try {
			rs = exceuteDbQuery("select * from currency where id=" + currId,
					"shiksha");
			if (getCountOfResultSet(rs) > 0) {
				while (rs.next()) {
					currName = rs.getString("currency_code");
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return currName;
	}

	public static String getCourseFeePeriod(String courseId) {
		String dbPeriod = null;
		try {
			rs = exceuteDbQuery(
					"select period from shiksha_courses_fees where period not in ('overall', 'otp') and course_id ="
							+ courseId + "  and status ='live' limit 1",
					"shiksha");
			if (getCountOfResultSet(rs) > 0) {
				while (rs.next()) {
					dbPeriod = rs.getString("period");
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return dbPeriod;
	}

	public static String getCourseFeeTotalIncludes(String courseId) {
		String dbPeriod = null;
		try {
			rs = exceuteDbQuery(
					"select total_includes from shiksha_courses_fees where course_id = "
							+ courseId
							+ " and listing_location_id = -1 and fees_type ='total' and fees_unit is not null and status ='live';",
					"shiksha");
			if (getCountOfResultSet(rs) > 0) {
				while (rs.next()) {
					dbPeriod = rs.getString("total_includes");
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return dbPeriod;
	}

	public static List<Integer> getExamListOfCourse(String courseId) {
		List<Integer> dbExamList = new ArrayList<Integer>();
		try {
			ResultSet rs = exceuteDbQuery(
					"select distinct exam_id from shiksha_courses_eligibility_exam_score where course_id="
							+ courseId
							+ " and status ='live' ;",
					"shiksha");
			if (getCountOfResultSet(rs) > 0) {
				while (rs.next()) {
					int examId = rs.getInt("exam_id");
					if(examId!=0)
					dbExamList.add(examId);
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return dbExamList;
	}

	public static ResultSet getCourseImportantDate(String courseId) {
		rs = exceuteDbQuery(
				"select * from shiksha_courses_important_dates where course_id = "
						+ courseId + " and status ='live';", "shiksha");
		return rs;
	}

	public static ResultSet getCourseExamDates(int examId) {
		rs = exceuteDbQuery(
				"select epcd.start_date, epcd.end_date, epcd.event_name, epm.id, epm.name"
						+ " from exampage_content_dates as epcd join exampage_main as epm "
						+ "on epcd.page_id = epm.exampageId where epm.id = "
						+ examId
						+ " and epm.status ='live' and epcd.status ='live' order by epcd.start_date asc;",
				"shiksha");
		return rs;
	}

	public static ResultSet getSeatsDateCatwise(String courseId) {
		rs = exceuteDbQuery(
				"select * from shiksha_courses_seats_breakup where course_id = "
						+ courseId
						+ " and breakup_by = 'category' and status ='live';",
				"shiksha");

		return rs;
	}

	public static ResultSet getSeatsDateExamwise(String courseId) {
		rs = exceuteDbQuery(
				"select epm.name, scsb.seats from exampage_main as epm join shiksha_courses_seats_breakup as scsb on epm.id = scsb.exam_id where scsb.course_id ="
						+ courseId
						+ " and scsb.status ='live' and scsb.breakup_by = 'exam' and epm.status='live';",
				"shiksha");

		return rs;
	}

	public static ResultSet getSeatsDateStatewise(String courseId) {
		rs = exceuteDbQuery(
				"select * from shiksha_courses_seats_breakup where course_id = "
						+ courseId
						+ " and breakup_by = 'domicile' and status ='live';",
				"shiksha");

		return rs;
	}

	public static ResultSet getRelatestatesInSeats(String courseId) {
		rs = exceuteDbQuery(
				"select * from shiksha_courses_seats_breakup where course_id = "
						+ courseId
						+ " and breakup_by = 'domicile' and category = 'related_state' and status ='live';",
				"shiksha");

		return rs;
	}

	public static ResultSet getCourseScholarShips(int ParentId,
			String ParentType) {
		rs = exceuteDbQuery(
				"select * from shiksha_institutes_scholarships where listing_id = "
						+ ParentId + " and listing_type = '" + ParentType
						+ "' and status ='live' order by  id asc;", "shiksha");

		return rs;
	}

	public static String getbaseAttributeListName(int i) {
		String name = null;
		try {
			rs = exceuteDbQuery(
					"select * From base_attribute_list where value_id =" + i
							+ " and status = 'live';", "shiksha");
			if (getCountOfResultSet(rs) > 0) {
				while (rs.next()) {
					name = rs.getString("value_name");
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return name;
	}

	public static ResultSet getCoursePlacementData(String courseId) {
		rs = exceuteDbQuery(
				"select * from shiksha_courses_placements_internships where course_id = "
						+ courseId
						+ " and type = 'placements' and salary_unit is not null  and status ='live';",
				"shiksha");
		return rs;
	}

	public static ResultSet getCourseInternData(String courseId) {
		rs = exceuteDbQuery(
				"select * from shiksha_courses_placements_internships where course_id = "
						+ courseId
						+ " and type = 'internship' and salary_unit is not null  and status ='live';",
				"shiksha");
		return rs;
	}

	public static ResultSet getCourseRecurtmentCompany(String courseId) {
		rs = exceuteDbQuery(
				"select cl.company_name, cl.logo_url from shiksha_courses_companies_mapping as sccm join company_logos as cl on sccm.company_id = cl.id where sccm.course_id ="
						+ courseId
						+ " and sccm.status ='live' and cl.status ='live' order by cl.company_name asc;",
				"shiksha");
		return rs;
	}

	public static String getExamname(String examId) {
		String examName = null;
		try {
			rs = exceuteDbQuery("select name from exampage_main where id="
					+ examId + " and status ='live';", "shiksha");
			if (getCountOfResultSet(rs) > 0) {
				while (rs.next()) {
					examName = rs.getString("name");
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return examName;

	}

	public static ResultSet getNaukriDataSalaryBucket(String courseId) {
		rs = exceuteDbQuery(
				"select nsd.exp_bucket, nsd.ctc50,  nsd.tot_emp from naukri_salary_data as nsd join shiksha_courses as sc on nsd.institute_id = sc.parent_id where sc.course_id= "
						+ courseId
						+ " and sc.status='live' order by exp_bucket asc;",
				"shiksha");
		return rs;
	}

	public static ResultSet getNaukriDataAlumniCountBySalaries(String courseId) {
		rs = exceuteDbQuery(
				"select sum(tot_emp) as sums from naukri_salary_data as nsd join shiksha_courses as sc "
						+ "on nsd.institute_id = sc.parent_id where sc.course_id="
						+ courseId + " and sc.status = 'live';", "shiksha");
		return rs;
	}

	public static ResultSet getNaurkiAlumniCountByPlacements(String courseId) {
		rs = exceuteDbQuery(
				"select sum(total_emp) as sums from naukri_alumni_stats as nsd join shiksha_courses as sc "
						+ "on nsd.institute_id = sc.parent_id where sc.course_id="
						+ courseId + " and sc.status = 'live';", "shiksha");
		return rs;
	}

	public static ResultSet getNaurkiSpecializationCompanyData(String courseId) {
		rs = exceuteDbQuery(
				"select specialization, comp_label, sum(total_emp) as count from naukri_alumni_stats as nas join shiksha_courses as sc on nas.institute_id = sc.parent_id where sc.course_id= "+courseId+" and sc.status='live' group by nas.specialization, nas.comp_label order by count desc ;",
				"shiksha");
		return rs;
	}

	public static ResultSet getNaurkiSpecializationfunctionalDate(
			String courseId) {
		rs = exceuteDbQuery(
				"select specialization, functional_area, sum(total_emp) as count from naukri_alumni_stats as nas join shiksha_courses as sc on nas.institute_id = sc.parent_id where sc.course_id= "
						+ courseId
						+ " and sc.status='live' group by nas.specialization, nas.functional_area;",
				"shiksha");
		return rs;
	}
	public static boolean getNaukricheckStatus(String courseId){
		boolean status =false;
		try{
		rs = exceuteDbQuery("select distinct base_course, education_type from shiksha_courses_type_information as scti join shiksha_courses as sc on scti.course_id = sc.course_id where sc.course_id = "+courseId+" and sc.status ='live' and scti.status='live';", "shiksha");
		if(getCountOfResultSet(rs)>0){
			while(rs.next()){
			String baseCourseString = String.valueOf(rs.getInt("base_course"));	
			String educationTypeString = String.valueOf(rs.getInt("education_type"));
			if(baseCourseString.equals("101") && educationTypeString.equals("20")){
				status = true;
			}
			}
		}
		}
		catch(SQLException e){
			e.printStackTrace();
		}
		return status;
	}
	
	public static ResultSet getAdmissionProcess(String courseId) {
		rs = exceuteDbQuery(
				"select * from shiksha_courses_admission_process where course_id="+courseId+" and status = 'live' order by stage_order;",
				"shiksha");

		return rs;
	}
}
