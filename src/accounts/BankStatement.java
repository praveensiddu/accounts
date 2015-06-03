package accounts;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class BankStatement {
	private String accountName = null;
	private BankConfig bc = null;

	private ArrayList<TR> trs = new ArrayList<TR>();

	private Map<String, TR> trMap = new HashMap<String, TR>();

	public BankStatement(String filename, String bankConfig, String accountName)
			throws IOException {
		if (accountName == null) {
			throw new IOException("Account no" + accountName);
		}
		this.setAccountName(accountName);
		bc = new BankConfig(bankConfig);

		FileReader fr = new FileReader(filename);
		BufferedReader br = new BufferedReader(fr);
		try {
			for (String line; (line = br.readLine()) != null;) {
				line = line.toLowerCase().trim();
				if (line.isEmpty())
					continue;
				if (skipLine(line))
					continue;

				TR tr = new TR(line, bc);
				makeUnique(tr);
				trs.add(tr);
			}
		} finally {
			if (br != null)
				try {
					br.close();
				} catch (IOException e) {
					// Ignore
				}
		}

	}

	public ArrayList<TR> getTrs() {
		return trs;
	}

	public void setTrs(ArrayList<TR> trs) {
		this.trs = trs;
	}

	private boolean skipLine(String line) {
		for (String s : bc.getIgnLineContains()) {
			if (line.contains(s)) {
				return true;
			}
		}
		for (String s : bc.getIgnLineStartsWith()) {
			if (line.startsWith(s)) {
				return true;
			}
		}
		return false;
	}

	public void makeUnique(TR tr) {
		String key = "" + tr.getDate();
		if (tr.getDesc() != null) {
			key += ", DESC=" + tr.getDesc();
		}
		if (trMap.containsKey(key)) {
			String key1 = key;
			int i = 1;
			for (; trMap.containsKey(key1); i++) {
				key1 = key + ", MKUNIQ" + i;
			}
			tr.setDesc(tr.getDesc() + ", MKUNIQ" + i);
			trMap.put(key1, tr);
		} else {
			trMap.put(key, tr);
		}

	}

	private static Map<String, Float> trTypeTotal(ArrayList<TR> art) {
		Map<String, Float> map = new HashMap<String, Float>();
		for (TR tr : art) {
			if (!map.containsKey(tr.getTrType())) {
				map.put(tr.getTrType(), tr.getDebit());
				continue;
			}
			Float f = map.get(tr.getTrType()) + tr.getDebit();
			map.put(tr.getTrType(), f);
		}
		return map;

	}

	private static void report(int year, BankStatement bs) {
		Map<String, ArrayList<TR>> map = new HashMap<String, ArrayList<TR>>();
		for (TR tr : bs.getTrs()) {

			Calendar cal = Calendar.getInstance();
			cal.setTime(tr.getDate());

			if (cal.get(Calendar.YEAR) != year) {
				continue;
			}
			if (!map.containsKey(tr.getProperty())) {
				ArrayList<TR> art = new ArrayList<TR>();
				art.add(tr);
				map.put(tr.getProperty(), art);
				continue;
			}
			ArrayList<TR> art = map.get(tr.getProperty());
			art.add(tr);
		}
		for (String key : map.keySet()) {
			System.out.println("Property=" + key);
			Map<String, Float> trTypeMap = trTypeTotal(map.get(key));
			for (String key1 : trTypeMap.keySet()) {
				System.out.println(key1 + "=" + trTypeMap.get(key1));
			}
			System.out.println("");
		}

	}

	private static void classify(BankStatement bs, TaxConfig tc) {
		for (String accountname : tc.getAccountsMap().keySet()) {
			if (!bs.getAccountName().equals(accountname)) {
				continue;
			}
			ArrayList<RuleRecord> arr = tc.getAccountsMap().get(accountname);
			for (TR tr : bs.getTrs()) {
				for (RuleRecord rr : arr) {
					if (rr.getDescContains() != null) {
						if (tr.getDesc().contains(rr.getDescContains())) {
							tr.setProperty(rr.getProperty());
							tr.setTaxCategory(rr.getTaxCategory());
							tr.setTrType(rr.getTrType());
							continue;
						}
					} else if (rr.getDescStartsWith() != null) {
						if (tr.getDesc().startsWith(rr.getDescStartsWith())) {
							tr.setProperty(rr.getProperty());
							tr.setTaxCategory(rr.getTaxCategory());
							tr.setTrType(rr.getTrType());
							continue;
						}
					}
				}

			}
		}

	}

	public String getAccountName() {
		return accountName;
	}

	public void setAccountName(String accountName) {
		this.accountName = accountName;
	}

	public String toString() {
		return toString(false);
	}

	public String toString(boolean nullOnly) {
		StringBuffer sb = new StringBuffer();
		for (TR tr : this.getTrs()) {
			if (nullOnly) {
				if (tr.getTaxCategory() != null)
					continue;
			}
			sb.append(tr.toString() + "\n");
		}
		return sb.toString();
	}

	public static void usage(final String err) {
		if (err != null && !"".equals(err)) {
			System.out.println(err);
		}
		System.out.println("Usage: -A action options\n");
		System.out
				.println("    -A parse -bankstatement <csvfile> -bankconfig <file> -accountname <n>\n");
		System.out
				.println("    -A parseandclassify -bankstatement <csvfile> -bankconfig <f> -accountname <n> -taxconfig <f>\n");
		System.exit(1);
	}

	public static final String PARSE = "parse";
	public static final String IMPORT2DB = "import2db";
	public static final String PARSEANDCLASSIFY = "parseandclassify";
	public static final Map<String, String> ALL_OPTS = new HashMap<String, String>();
	static {
		ALL_OPTS.put("A", Getopt.CONTRNT_S);
		ALL_OPTS.put("bankstatement", Getopt.CONTRNT_S);
		ALL_OPTS.put("bankconfig", Getopt.CONTRNT_S);
		ALL_OPTS.put("accountname", Getopt.CONTRNT_S);
		ALL_OPTS.put("taxconfig", Getopt.CONTRNT_S);
	}

	public static void main(String[] args) {

		Map<String, String> argHash = Getopt.processCommandLineArgL(args,
				ALL_OPTS, true);
		if (argHash.get(Getopt.ERROR) != null) {
			usage((String) argHash.get(Getopt.ERROR));
		}

		String action = argHash.get("A");
		try {
			if (PARSE.equalsIgnoreCase(action)) {
				BankStatement bs = new BankStatement(
						argHash.get("bankstatement"),
						argHash.get("bankconfig"), argHash.get("accountname"));
				System.out.println("" + bs);

			} else if (IMPORT2DB.equalsIgnoreCase(action)) {
				BankStatement bs = new BankStatement(
						argHash.get("bankstatement"),
						argHash.get("bankconfig"), argHash.get("accountname"));

				DBImpl dbi = new DBImpl();
				Connection conn = dbi.createDB(null, null);
				dbi.importBankStatement(conn, bs);
			} else if (PARSEANDCLASSIFY.equalsIgnoreCase(action)) {
				BankStatement bs = new BankStatement(
						argHash.get("bankstatement"),
						argHash.get("bankconfig"), argHash.get("accountname"));
				TaxConfig tc = new TaxConfig(argHash.get("taxconfig"));
				classify(bs, tc);
				System.out.println("" + bs.toString(true));
				report(2014, bs);

			} else {
				usage("Invalid action");
			}
		} catch (IOException | SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
