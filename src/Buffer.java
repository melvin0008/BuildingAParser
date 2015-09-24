import java.io.BufferedReader;
import java.io.DataInputStream;

public class Buffer {
	private boolean end;
	private String line = "";
	private int position = 0;
	private DataInputStream inStream;

	public Buffer(DataInputStream dataInputStream) {
		this.inStream = dataInputStream;
	} // Buffer

	@SuppressWarnings("deprecation")
	public char getChar() {
		position++;
		if (position >= line.length()) {
			try {
				line = inStream.readLine();
			} catch (Exception e) {
				System.err.println("Invalid read operation");
				System.exit(1);
			}
			if (line.equals("end"))
				end = true;
				position = 0;
			// System.out.println(line);
			line = line + "\n";
		}
		return line.charAt(position);
	}
	public boolean isEnd() {
		return end;
	}

} // class Buffer

