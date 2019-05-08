package au.edu.envirotech.tasktracker.services;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import au.edu.envirotech.tasktracker.model.Task;
import au.edu.envirotech.tasktracker.model.User;

public class PersistenceService {
	
	private static Connection getConnection() throws SQLException {
		
		try {
			Class.forName("org.postgresql.Driver");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		
		Connection connection = DriverManager.getConnection("jdbc:postgresql://localhost:5432/envirotech",
				"envirotech", "Burleigh7u8i9o0p");
		
		return connection;
	}

	public static int registerUser(String email, String password) throws SQLException {
		
		PreparedStatement preparedStatement = null;
		String sql = "insert into public.user (id, email, password) values (nextval('user_seq'), ?, ?);"; 
		int newId = 0;
		
		try {
			
			preparedStatement = getConnection().prepareStatement(sql);
			
			preparedStatement.setString(1, email);
			preparedStatement.setString(2, password);
			
			newId = preparedStatement.executeUpdate();
			
		} catch (SQLException e) {
			e.printStackTrace();
			throw e;
		} finally {
			preparedStatement.close();
		}
		
		return newId;
	}
	
	public static User getAuthorizedUser(String email, String password) throws SQLException {
		
		String sql = "select * from public.user where email like ? and password like ?";
		PreparedStatement preparedStatement = null;
		ResultSet resultSet;
		
		try {
			
			preparedStatement = getConnection().prepareStatement(sql);
			
			preparedStatement.setString(1, email);
			preparedStatement.setString(2, password);
			
			resultSet = preparedStatement.executeQuery();
			
			if (resultSet != null && resultSet.next() && resultSet.getInt("id") > 0) {
				return new User(resultSet.getInt("id"), resultSet.getString("email"));
			}
			
		} catch (SQLException e) {
			
			e.printStackTrace();
			throw e;
			
		} finally {
			
			if (preparedStatement != null) {
				preparedStatement.close();
			}
		}
		
		return null;
	}

	public static void saveTaskList(List<Task> taskList) throws SQLException {

		String stringDelete = "delete from public.task where user_id = ? and id not in ("; // TODO delete absent records
		String stringInsert = "insert into public.task (\"id\", \"user_id\", \"date\" ) values (nextval('task_seq'), ?, ?);";
		String stringUpdate = ""; // TODO insert new records

		PreparedStatement preparedStatementInsert = null;
		PreparedStatement preparedStatementUpdate = null;
		PreparedStatement preparedStatementDelete = null;

		// *insert new tasks
		// *update tasks
		// *delete absent tasks

		try {

			preparedStatementInsert = getConnection().prepareStatement(stringInsert);
			preparedStatementUpdate = getConnection().prepareStatement(stringUpdate);

			// taking the present ids to be kept
			for (Task task : taskList) {
				stringDelete += task.getId() + ",";
			}
			
			// finishing the deletion string
			stringDelete = stringDelete.substring(0, stringDelete.length() - 1) + ");";	// cuts the last "," and closes the parenthesis

			// deleting records of tasks removed from the list
			preparedStatementDelete = getConnection().prepareStatement(stringDelete);
			preparedStatementDelete.setInt(1, taskList.get(0).getUser().getId());
			preparedStatementDelete.executeUpdate();

			for (Task task : taskList) {

				if (task.getId() == 0) { // inserting the new task

					preparedStatementInsert.setInt(1, task.getUser().getId());
					preparedStatementInsert.setDate(2, new Date(task.getDate().getTime()));
					preparedStatementInsert.executeUpdate();

				} else { // updating the existing task

				}
			}

		} catch (SQLException e) {

			getConnection().rollback();
			e.printStackTrace();
			throw e;

		} finally {

			if (preparedStatementInsert != null) preparedStatementInsert.close();
			if (preparedStatementUpdate != null) preparedStatementUpdate.close();
			if (preparedStatementDelete != null) preparedStatementDelete.close();
		}
	}

	public static List<Task> findTaskListByUser(User user) throws SQLException {
		
		String stringSql = user != null ? "select * from task where user_id = " + user.getId() : "select t.*, u.email from task t join \"user\" u on u.id = t.user_id;"; 
		ResultSet resultSet = getConnection().prepareStatement(stringSql).executeQuery();
		
		if (resultSet != null) {
			
			ArrayList<Task> taskList = new ArrayList<Task>();
			
			while(resultSet.next()) {
				
				Task t = new Task();
				
				t.setUser(user != null ? user : new User(resultSet.getInt("user_id"), resultSet.getString("email")));
				t.setId(resultSet.getInt("id"));
				t.setDate(new Date(resultSet.getDate("date").getTime()));
				
				taskList.add(t);
			}
			
			return taskList;
		}
		
		return null;
	}

	public static List<User> findUserById(Integer... idArray) throws SQLException {
		
		List<User> userList = null;
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		String stringSql = "select * from public.user";
		
		if (idArray.length > 0) {
			
			stringSql += " where id in (";
			
			for (Integer integer : idArray) {
				stringSql += integer + ",";
			}
			
			stringSql = stringSql.substring(0, stringSql.length() - 1);
			stringSql += ")";
		}
		
		try {

			preparedStatement = getConnection().prepareStatement(stringSql);
			resultSet = preparedStatement.executeQuery();
			
			if (resultSet != null) {
				
				userList = new ArrayList<User>();
				
				while (resultSet.next()) {
					userList.add(new User(resultSet.getInt("id"), resultSet.getString("email")));
				}
			}
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			preparedStatement.close();
			resultSet.close();
		}
		
		return userList;
	}
}
