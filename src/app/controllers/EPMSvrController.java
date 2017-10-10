package app.controllers;

import java.util.*;

import app.models.*;
import app.models.Set;
import org.javalite.activejdbc.LazyList;
import org.javalite.activejdbc.Model;
import org.javalite.activeweb.annotations.POST;

public class EPMSvrController extends BasicController 
{
	public void index()
	{
		respond("This is EPMSvr");
	}

	private long countSetWhere(){
		String kw = param("keyword").trim();
		long total=Set.count();
		if( !kw.isEmpty() )
		{
			total = Set.count("DS_NAME LIKE ? or DS_CODE LIKE ?",kw,kw);
		}
		return total;
	}

	public void allSets()
	{
		long total = countSetWhere();
		int pagesize = 10;
		if( ! param("pagesize").isEmpty())
		{
			pagesize = Integer.parseInt(param("pagesize"));
			if(pagesize==0 || pagesize>1000)
			{
				pagesize = 10;
			}
		}
		int offset = 0;
		if( ! param("p").isEmpty())
		{
			offset = (Integer.parseInt(param("p"))-1)*pagesize;
		}
		String kw = param("keyword").trim();
		System.out.println(kw);
		LazyList<Model> setList=Set.where("DS_NAME LIKE ? or DS_CODE LIKE ?","%"+kw+"%","%"+kw+"%")
					.orderBy("UPDATED_AT desc").limit(pagesize).offset(offset);

		List<Map<String,Object>> Nodes = setList.toMaps();
		for (Map<String,Object> Node:Nodes) {
			Node.put("META_ROWS",Set2Element.count("DS_CODE = ?",Node.get("DS_CODE")));
		}
		output(0, Nodes, Math.ceil(total/pagesize));
	}

	@POST
	public void save()throws Exception
	{
		Map<String, Object> rs = new HashMap<String, Object>();
		Map params = params1st();
		String ID = param("ID");

		if (ID==null || Integer.parseInt(ID)==0) {
			params.remove("ID");
		}
		Element m = new Element();
		m.fromMap(params);
		if (m.save()) {
			echo(0, m.toMap());
		} else {
			echo(10001, "出错了");
		}
	}

	public void remove() {
		try {
			String ids = param("idsStr");
			int c = Element.delete("ID in("+ids+")");
			echo(0, "删除"+c+"条记录");
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public void getOptions() {
		
	}

	@POST
	public void allElements() {
		String kw = param("keyword");
		String query = "";
		Object[] params =new Object[4];
		LazyList s2eList = Set2Element.where("DS_CODE = ?",param("sid"));
		List<Map> s2eMaps=s2eList.toMaps();
		String[] mids = new String[s2eList.size()];
		int i=0;
		for(Map item :  s2eMaps) {
			mids[i] = item.get("DS_VS_CODE").toString();
			++i;
		}
		query="METADATA_IDENTIFY in ( ? ) or METADATA_IDENTIFY = ? or METADATA_NAME like ? or pid = ? or pid=0";
		params[0] = mids;
		params[1] = kw;
		params[2] = "%"+kw+"%";
		params[3] = param("pid");
		long total = Element.count(query,params);

		int pagesize = 10;
		if( null != param("pagesize"))
		{
			pagesize = Integer.parseInt(param("pagesize"));
			if(pagesize<0 || pagesize>1000)
			{
				pagesize = 10;
			}
		}
		int offset = 0;
		if( null != param("p"))
		{
			offset = (Integer.parseInt(param("p"))-1)*pagesize;
		}
		LazyList eleList = Element.where(query,params)
				.limit(pagesize).offset(offset).orderBy("VERSION_DATE DESC");
		List<Map> eleMaps = eleList.toMaps();
		ArrayList data = new ArrayList();
		for(Map eleMap:eleMaps)
		{
			LazyList vopList=VOption.where("FIELDCODE_TABLECODE = ?", eleMap.get("FIELDCODE_TABLECODE"))
					.orderBy("FIELDORDER ASC");
			List<Map> vopMaps = vopList.toMaps();
			if(vopMaps.size()>0)
			{
				eleMap.put("data",vopMaps);
			}

			//读单位
			if( null != eleMap.get("DATA_FEATURE_ID"))
			{
				Unit unit =Unit.findById(eleMap.get("DATA_FEATURE_ID"));
				if(null != unit)
				{
					eleMap.remove("DATA_UNIT");
					eleMap.put("DATA_UNIT",unit.get("DATA_UNIT"));
				}
			}

			data.add(eleMap);
		}
		output(0, data, Math.ceil(total/pagesize));
	}
}
