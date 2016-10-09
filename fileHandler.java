import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class fileHandler {
	private Connection conn = null;
	private PreparedStatement statement = null;

	public void connSQL() {
		String url = "jdbc:mysql://192.168.1.117:3306/zabbix?characterEncoding=UTF-8";
		String username = "zabbix";
		String password = "zabbix";
		try {
			Class.forName("com.mysql.jdbc.Driver");
			conn = DriverManager.getConnection(url, username, password);

		} catch (ClassNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public boolean insetSql(String sql) {
		try {
//			System.out.println("插入中");
			statement = conn.prepareStatement(sql);
			statement.executeUpdate();
			return true;
		} catch (SQLException e) {
//			System.out.println("插入数据库时出错：");
			e.printStackTrace();
		} catch (Exception e) {
//			System.out.println("插入时出错：");
			e.printStackTrace();
		}
		return false;
	}

	void deconnSQL() {
		try {
			if (conn != null)
				conn.close();
		} catch (Exception e) {
//			System.out.println("关闭数据库问题 ：");
			e.printStackTrace();
		}
	}

	String querySql(String sql) {
		try {
			statement = conn.prepareStatement(sql);
			ResultSet rs = statement.executeQuery();
			String out = "";
			while (rs.next()) {
				out = rs.getString(1);
				// System.out.println(out);
			}
			return out;
		} catch (Exception e) {
//			System.out.println("查询出错");
			e.printStackTrace();
			return null;
		}
	}

	public static String getTime(String s2) {
		String[] s3 = s2.split("/");
		String yyyy = s3[0];
		String MM = s3[1];
		String dd = s3[2].split(" ")[0];
		String time = s3[2].split(" ")[1];
		return yyyy + "-" + MM + "-" + dd + " " + time;
	}

	public static void main(String[] args) {
		try {
			// String path = "/home/shc/DZG/IPC_Camera/almLog";
//			String path = "E:/almLog.txt";
//			String hostip = "192.168.1.70";
			 String path = args[0];
			 String hostip = args[1];

			FileReader fr = new FileReader(path);
			BufferedReader br = new BufferedReader(fr);

			List<String> list = new ArrayList<>();
			String line;
			String newFileLine = "";
			fileHandler fileHandler = new fileHandler();
			fileHandler.connSQL();
			while ((line = br.readLine()) != null) {
				String lineAry[];
				lineAry = line.split(";");

				for (String b : lineAry) {
					String a[] = b.split("-");
					String deviceType = "ipc";
					String sql = "select ipc_host from nvr_ipc where nvr_host='"
							+ a[0] + "' and channel='" + a[4] + "'";
					String ipcHost = fileHandler.querySql(sql);
					String clock = fileHandler.getTime(a[2]);
					String alarmType = a[1];
					String sql1 = "select hostid from hosts where host= '"
							+ ipcHost + "' ";
					String hostid = fileHandler.querySql(sql1);
					if (hostid == null || hostid.equals("")) {// 设备不存在的时候，初始化ID为0
						hostid = "" + 0;
					}
					String querySql1 = "select hostid from alarm_tab where deviceType='"
							+ deviceType
							+ "' and alarmType='"
							+ alarmType
							+ "' and clock='"
							+ clock
							+ "' and hostid='"
							+ hostid + "' and host='" + ipcHost + "' ";
					String result = fileHandler.querySql(querySql1);// 根据result确定数据是否重复
//					if (result == null || result.equals("")) {// 判断插入数据是否重复
//						String insertSql = "insert into historty_text(deviceType,alarmType,clock,hostid,host) values('"
//								+ deviceType
//								+ "' ,'"
//								+ alarmType
//								+ "' , '"
//								+ clock
//								+ "','"
//								+ hostid
//								+ "','"
//								+ ipcHost
//								+ "')";
//						fileHandler.insetSql(insertSql);
//					}
					if (result == null || result.equals("")) {// 判断插入数据是否重复
						String insertSql = "insert into alarm_tab(deviceType,alarmType,clock,hostid,host,confirm) values('"
								+ deviceType
								+ "' ,'"
								+ alarmType
								+ "' , '"
								+ clock
								+ "','"
								+ hostid
								+ "','"
								+ ipcHost
								+ "','"+0+"')";
						fileHandler.insetSql(insertSql);
					}
					if (hostip.equals(a[0].replace(" ", ""))) {// 判断是否是指定的nvr,只提取需要的信息,用于更新录像机的最新数据
						list.add(b.replace(a[0] + "-", ""));

					} else {
						newFileLine += b + ";";
					}
				}
				line = line.replace(" ", "");
			}
			String out = "";
			int i = 0;
			for (String s : list) {
				// System.out.println(s);
				if (i == 0) {
					out = s;
				} else {
					out = out + ";" + s;
				}
				i++;
			}
			System.out.println(out);
			// System.out.println("返回信息：" + out);
			// System.out.println("剩余信息：" + newFileLine);

			fileHandler.deconnSQL();
			File f = new File(path);
			FileWriter fw = new FileWriter(f);
			fw.write(newFileLine);
			fw.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
