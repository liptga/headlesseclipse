package com.ind.eclipse.headlessworkspace;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;

public class BuildLogReader extends Thread
{
	private File f;
	private static PrintStream out = System.out;
	
	public BuildLogReader(File f)
	{
		setDaemon(true);
		this.f = f;
	}
	
	@Override
	public void run()
	{
		FileInputStream fis = null;
		try
		{
			out.println();
			if (!f.createNewFile())
			{
				out.println("ANT LOG: FILE CANNOT BE CREATED: " + f);
			}
			out.println("ANT LOG: Listening on file: " + f);
			out.println();
			fis = new FileInputStream(f);
			BufferedReader br = new BufferedReader(new InputStreamReader(fis));
			while (true)
			{
				String line = br.readLine();
				if (line == null)
				{
					sleep(100);
				}
				else
				{
					out.println("ANT LOG: " + line);
				}
			}
		}
		catch (InterruptedException ie)
		{
			out.println("ANT LOG: END OF FILE");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		if (fis != null)
		{
			try
			{
				fis.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}
}