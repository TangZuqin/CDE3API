package app.controllers;

import app.models.*;
import org.javalite.activejdbc.Base;
import org.javalite.activejdbc.LazyList;
import org.javalite.activejdbc.Model;
import org.javalite.activeweb.annotations.POST;
import org.javalite.common.JsonHelper;

import java.lang.reflect.Array;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class EAliasSvrController extends BasicController
{
	public void index()
	{
		respond("This is EAliasSvr");
	}

	@POST
	public void aliasSave () throws Exception {
		Map params = params1st();
		Map<String,Object> ealias = new HashMap<>();
		Map<String,Object> newEalias = new HashMap<>();
		String ALIAS = java.net.URLDecoder.decode(param("ALIAS"),"utf-8");
		System.out.println(param("ALIAS"));
		System.out.println(ALIAS);
		Date date=new Date();
		DateFormat format=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String time=format.format(date);

		ealias.put("TEMPLATE_ID",param("TEMPLATE_ID"));
		ealias.put("ALIAS",ALIAS);
		ealias.put("DATATYPE",param("DATATYPE"));
		ealias.put("TIMEI",time);
		ealias.put("TIMEU",time);
		TplElement tplelement =new TplElement();
		tplelement.fromMap(ealias);
		tplelement.save();

		int GETID = Integer.parseInt(tplelement.getId().toString());
		String strId = tplelement.getId().toString();
		TplElement e = TplElement.findFirst("INT_ID = ?", GETID);
		e.set("ELEMENT_ID", strId, "ID", strId+"_"+param("TEMPLATE_ID").toString()).saveIt();

		ArrayList array_with_id = new ArrayList();
		Map[] voptions =JsonHelper.toMaps(params.get("voptions").toString());
		System.out.println(params.get("voptions").toString());
		for (Map<String,Object> option: voptions) {
			option.put("TIMEI",time);
			option.put("TIMEU",time);
			option.put("EALIAS_ID",strId+"_"+param("TEMPLATE_ID").toString());
			option.put("TEMPLATE_ID",param("TEMPLATE_ID"));
			TplOption tploption =new TplOption();
			tploption.fromMap(option);
			tploption.save();
			array_with_id.add(option);
		}
		ealias.put("INT_ID",GETID);
		ealias.put("ID",strId+"_"+param("TEMPLATE_ID").toString());
		if(array_with_id.size() >0 ){
			ealias.put("voptions",array_with_id);
		}
		output(0,ealias);
	}

	@POST
	public void save () throws Exception {
		Map<String, Object> rs = new HashMap<String, Object>();
		Map params = params1st();
		String id = param("id");
		if (id.isEmpty()) {
			output(20001,"参数异常");
		}
		String[] ids = id.split("_");
		if (ids.length<2) {
			output(11002,"别元格式错误:"+id);
		}
		Template template = Template.findById(ids[1]);
		Map<String,Object> Node = template.toMap();
		int zid= Integer.parseInt(Node.get("ZID").toString());
		int pid= Integer.parseInt(Node.get("PID").toString());
		long total = TplElement.count("ID = ?",id);
		Map<String,Object> ealias = new HashMap<>();
		Date date=new Date();
		DateFormat format=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String time=format.format(date);
		System.out.println(time);
		if(total >0) {
			ealias.put("ID",id);
			ealias.put("ALIAS",params.get("alias"));
			ealias.put("DATATYPE",params.get("datatype").equals(null)?"":params.get("datatype"));
			ealias.put("ZID",zid);
			ealias.put("PROJECT_ID",pid);
			ealias.put("TIMEU",time);
		}else{
			ealias.put("ID",id);
			ealias.put("ELEMENT_ID",ids[0]);
			ealias.put("TEMPLATE_ID",ids[1]);
			ealias.put("ALIAS",params.get("alias"));
			ealias.put("DATATYPE",params.get("datatype").equals(null)?"":params.get("datatype"));
			ealias.put("ZID",zid);
			ealias.put("PROJECT_ID",pid);
			ealias.put("TIMEI",time);
			ealias.put("TIMEU",time);
		}
		TplElement tplelement =new TplElement();
		tplelement.fromMap(ealias);
		tplelement.save();
		ArrayList array_with_id = new ArrayList();
		Map[] voptions =JsonHelper.toMaps(params.get("voptions").toString());
		System.out.println(params.get("voptions").toString());
		for (Map<String,Object> option: voptions) {
			Map data = new HashMap();
			data.put("ALIAS",option.get("alias"));
			data.put("AVALUE",option.get("avalue"));
			data.put("SORTER",option.get("sorter"));
			data.put("STATUS",option.get("status"));
			data.put("TIMEU",time);
			if(null != option.get("id")){
				data.put("EALIAS_ID",params("id"));
				data.put("VOPTION_ID",option.get("voption_id"));
				data.put("TEMPLATE_ID",ids[1]);
				data.put("TIMEI",time);
			}else{
				data.put("ID",option.get("id"));
				data.put("CVALUE",option.get("cvalue"));
			}
			TplOption tploption =new TplOption();
			tploption.fromMap(data);
			tploption.save();
			array_with_id.add(data);
		}
		if(array_with_id.size() >0 ){
			ealias.put("voptions",array_with_id);
		}
		output(0,ealias);
	}
}
