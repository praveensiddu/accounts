package accounts;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BankConfig {
	public static final String IGNORE_LINES_STARTSWITH = "ignore_lines_startswith";
	public static final String IGNORE_LINES_CONTAINS = "ignore_lines_contains";

	public static final String DATE_COl = "date_col";
	public static final String DESCRIPTION_COL = "description_col";
	public static final String MEMO_COL = "memo_col";
	public static final String AMOUNT_DEBIT_COL = "amount_debit_col";
	public static final String AMOUNT_CREDIT_COL = "amount_credit_col";
	public static final String CHECK_NUMBER_COL = "check_number_col";
	public static final String FEES_COL = "fees_col";
	public static final String DATE_FORMAT = "date_format";

	private List<String> ignLineStartsWith = new ArrayList<String>();

	public List<String> getIgnLineStartsWith() {
		return ignLineStartsWith;
	}

	public void setIgnLineStartsWith(List<String> ignLineStartsWith) {
		this.ignLineStartsWith = ignLineStartsWith;
	}

	private List<String> ignLineContains = new ArrayList<String>();

	public List<String> getIgnLineContains() {
		return ignLineContains;
	}

	public void setIgnLineContains(List<String> ignLineContains) {
		this.ignLineContains = ignLineContains;
	}

	private int dateIndex = -1;
	private int descIndex = -1;
	private int memoIndex = -1;
	private int debitIndex = -1;
	private int creditIndex = -1;
	private int checkNoIndex = -1;
	private int feesIndex = -1;
	private String dateFormat;

	public BankConfig(String file) throws IOException {
		FileReader fr = new FileReader(file);
		BufferedReader br = new BufferedReader(fr);
		try {
			for (String line; (line = br.readLine()) != null;) {
				line = line.toLowerCase().trim();
				if (line.isEmpty())
					continue;
				if (line.startsWith("#"))
					continue;

				String[] fields = line.split("=");
				if (fields.length != 2) {
					throw new IOException("Invalid line=" + line);
				}
				if (line.startsWith(DATE_FORMAT)) {
					setDateFormat(fields[1].trim());
				} else if (line.startsWith(IGNORE_LINES_STARTSWITH)) {
					ignLineStartsWith.add(fields[1].trim());
				} else if (line.startsWith(IGNORE_LINES_CONTAINS)) {
					ignLineContains.add(fields[1].trim());
				} else if (line.startsWith(DATE_COl)) {
					setDateIndex(new Integer(fields[1].trim()).intValue() - 1);
				} else if (line.startsWith(DESCRIPTION_COL)) {
					setDescIndex(new Integer(fields[1].trim()).intValue() - 1);
				} else if (line.startsWith(MEMO_COL)) {
					setMemoIndex(new Integer(fields[1].trim()).intValue() - 1);
				} else if (line.startsWith(AMOUNT_DEBIT_COL)) {
					setDebitIndex(new Integer(fields[1].trim()).intValue() - 1);
				} else if (line.startsWith(AMOUNT_CREDIT_COL)) {
					setCreditIndex(new Integer(fields[1].trim()).intValue() - 1);
				} else if (line.startsWith(CHECK_NUMBER_COL)) {
					setCheckNoIndex(new Integer(fields[1].trim()).intValue() - 1);
				} else if (line.startsWith(FEES_COL)) {
					setFeesIndex(new Integer(fields[1].trim()).intValue() - 1);
				}
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

	public int getDateIndex() {
		return dateIndex;
	}

	public void setDateIndex(int dateIndex) {
		this.dateIndex = dateIndex;
	}

	public int getDescIndex() {
		return descIndex;
	}

	public void setDescIndex(int descIndex) {
		this.descIndex = descIndex;
	}

	public int getMemoIndex() {
		return memoIndex;
	}

	public void setMemoIndex(int memoIndex) {
		this.memoIndex = memoIndex;
	}

	public int getDebitIndex() {
		return debitIndex;
	}

	public void setDebitIndex(int debitIndex) {
		this.debitIndex = debitIndex;
	}

	public int getCreditIndex() {
		return creditIndex;
	}

	public void setCreditIndex(int creditIndex) {
		this.creditIndex = creditIndex;
	}

	public int getCheckNoIndex() {
		return checkNoIndex;
	}

	public void setCheckNoIndex(int checkNoIndex) {
		this.checkNoIndex = checkNoIndex;
	}

	public int getFeesIndex() {
		return feesIndex;
	}

	public void setFeesIndex(int feesIndex) {
		this.feesIndex = feesIndex;
	}

	public String getDateFormat() {
		return dateFormat;
	}

	public void setDateFormat(String dateFormat) {
		this.dateFormat = dateFormat;
	}

	public static void main(String[] args) {
		try {
			BankConfig bc = new BankConfig(args[0]);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
