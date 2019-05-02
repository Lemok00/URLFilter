package filter;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.net.URL;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TimerTask;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.spi.DirStateFactory.Result;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.JTextComponent;
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter;
import javax.swing.text.Highlighter;
import javax.swing.text.Highlighter.Highlight;
import javax.swing.text.Highlighter.HighlightPainter;

public class URLfilter extends JFrame {
	public static void main(String[] args) {
		new normalFilter();
	}
}

class normalFilter extends JFrame {
	JPanel mainPanel = new JPanel();
	JPanel buttonPanel = new JPanel();
	JButton jButton1 = new JButton("导入敏感词库");
	JButton jButton2 = new JButton("高亮显示敏感词");
	JButton jButton3 = new JButton("爬取目标URL");
	JButton jButton4 = new JButton("导入目标URL库");
	JButton jButton5 = new JButton("开始批量爬取");
	JLabel jLabel = new JLabel("请导入敏感词库");
	JLabel urLabel = new JLabel("请导入目标URL库");
	JTextArea jTextArea = new JTextArea();
	JScrollPane jScrollPane = new JScrollPane(jTextArea);
	JProgressBar jProgressBar = new JProgressBar();
	normalFilter self = this;
	String fileName = null;
	String source = null;
	String outputPath = null;
	Vector<String> urlvector = null;
	Vector<String> wordvector = null;
	AtomicInteger count = new AtomicInteger(0);

	public normalFilter() {
		this.setTitle("URL Filter");
		this.setSize(800, 600);
		this.setLocation(200, 200);
		this.setVisible(true);
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		this.add(mainPanel);
		mainPanel.setLayout(new BorderLayout());
		mainPanel.add(jScrollPane, BorderLayout.CENTER);
		mainPanel.add(buttonPanel, BorderLayout.SOUTH);
		buttonPanel.setLayout(new GridLayout(4, 2));
		buttonPanel.add(jButton3);
		buttonPanel.add(jButton2);
		buttonPanel.add(jButton1);
		buttonPanel.add(jLabel);
		buttonPanel.add(jButton4);
		buttonPanel.add(urLabel);
		buttonPanel.add(jButton5);
		buttonPanel.add(jProgressBar);
		jTextArea.setLineWrap(true);
		jTextArea.setWrapStyleWord(true);
		jTextArea.setEditable(false);
		jButton2.setEnabled(false);
		jButton4.setEnabled(false);
		jButton5.setEnabled(false);

		jButton1.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				Vector<JButton> bVector = new Vector<>();
				bVector.add(jButton2);
				bVector.add(jButton4);
				wordvector = filter.importLib(bVector, jLabel, self);
				jButton5.setEnabled(false);
			}
		});

		jButton2.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				filter.highLight(jTextArea, wordvector);
			}
		});

		jButton3.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				String source = JOptionPane.showInputDialog(self, "请输入目标URL");
				// 获取url
				jButton3.setEnabled(false);
				new Thread(new Runnable() {

					@Override
					public void run() {
						try {
							jProgressBar.setIndeterminate(true);
							jProgressBar.setStringPainted(false);
							jTextArea.setText("");
							jTextArea.append(filter.deleteHTMLTag(filter.getSource(source)));
							JOptionPane.showMessageDialog(self, source + "爬取完成", "网页爬取完成", JOptionPane.PLAIN_MESSAGE);
						} catch (Exception e2) {
							JOptionPane.showMessageDialog(self, "目标URL异常", "无法提取网页信息", JOptionPane.ERROR_MESSAGE);
							jButton2.setEnabled(false);
						}finally {
							jProgressBar.setIndeterminate(false);
							jProgressBar.setMaximum(1);
							jProgressBar.setValue(0);
							jProgressBar.setBorderPainted(false);
							jButton3.setEnabled(true);
						}
					}
				}).start();
			}
		});

		jButton4.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				Vector<JButton> bVector = new Vector<>();
				bVector.add(jButton5);
				urlvector = filter.importLib(bVector, urLabel, self);
			}
		});

		jButton5.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				jButton5.setEnabled(false);
				JFileChooser jFileChooser = new JFileChooser();
				jFileChooser.setCurrentDirectory(new File("."));
				jFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				jFileChooser.showDialog(new Label(), "选择输出文件夹");
				outputPath = jFileChooser.getSelectedFile().getAbsolutePath();
				if (outputPath == null) {
					return;
				}
				new Thread(new Runnable() {

					@Override
					public void run() {
						count.set(0);
						jProgressBar.setIndeterminate(false);
						jProgressBar.setMinimum(0);
						jProgressBar.setMaximum(urlvector.size());
						jProgressBar.setValue(0);
						jProgressBar.setStringPainted(true);
						HashMap<Integer, Vector<String>> map = new HashMap<>();
						int pronum = 3;
						for (int i = 0; i < pronum; i++) {
							map.put(i, new Vector<>());
						}
						for (int i = 0; i < urlvector.size(); i++) {
							int j = i % pronum;
							map.get(j).add(urlvector.elementAt(i));
						}
						for (int i = 0; i < pronum; i++) {
							if (map.get(i).size() != 0) {
								new scan(map.get(i), self);
							}
						}
						int c = 0;
						AtomicInteger c1 = new AtomicInteger(0);
						new Thread(new Runnable() {
							@Override
							public void run() {
								int c2;
								do {
									c2 = c1.get();
									Dimension dimension = jProgressBar.getSize();
									Rectangle rectangle = new Rectangle(0, 0, dimension.width, dimension.height);
									jProgressBar.setValue(c2);
									jProgressBar.paintImmediately(rectangle);
									try {
										Thread.sleep(1000);
									} catch (InterruptedException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
								} while (c2 != urlvector.size());
							}
						}).start();
						while (true) {
							c = count.get();
							c1.set(c);
							if (c == urlvector.size()) {
								JOptionPane.showMessageDialog(self, "所有URL过滤完成", "过滤完成", JOptionPane.PLAIN_MESSAGE);
								jButton5.setEnabled(true);
								break;
							}
							try {
								Thread.sleep(1000);
							} catch (InterruptedException e1) {

							}
						}
					}
				}).start();
			}

			class scan implements Runnable {
				Vector<String> vector = null;
				normalFilter nFilter = null;

				public scan(Vector<String> v, normalFilter nf) {
					vector = v;
					nFilter = nf;
					new Thread(this).start();
				}

				@Override
				public void run() {
					for (String string : vector) {
						BufferedWriter bWriter = null;
						try {
							Pattern pattern = Pattern.compile("\\w+");
							Matcher matcher = pattern.matcher(string);
							String filename = "";
							while (matcher.find()) {
								filename += matcher.group();
							}
							File file = new File(outputPath + "/" + filename + ".txt");
							if (!file.exists()) {
								file.createNewFile();
							}
							bWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
							String source = filter.getSource(string);
							source = filter.deleteHTMLTag(source);
							bWriter.write(source);
							bWriter.newLine();
							for (String word : wordvector) {
								if ((source.indexOf(word)) >= 0) {
									bWriter.write(word);
									bWriter.newLine();
								}
							}
						} catch (Exception e) {
							try {
								bWriter.write("当前网址出现异常，无法爬取");
							} catch (IOException e1) {
							}
						} finally {
							nFilter.count.incrementAndGet();
							try {
								bWriter.close();
							} catch (IOException e) {
							}
						}
					}
				}
			}
		});
	}
}

class filter {
	public static String getSource(String source) throws Exception {
		String result = "";
		URL url = new URL(source);
		BufferedReader bReader = new BufferedReader(new InputStreamReader(url.openStream(), "utf-8"));
		String line;
		while ((line = bReader.readLine()) != null) {
			result += line;
		}
		bReader.close();
		return result;
	}

	public static String deleteHTMLTag(String source) {
		String regEx_script = "<script[^>]*?>[\\s\\S]*?<\\/script>"; // 定义script的正则表达式
		String regEx_style = "<style[^>]*?>[\\s\\S]*?<\\/style>"; // 定义style的正则表达式
		String regEx_html = "<[^>]+>"; // 定义HTML标签的正则表达式
		String regEx_zhuanyi = "&[\\s\\S]*?;"; // 定义转义字符&copys;等的正则表达式
		String regEx_space = "\\s{2,}|\t";

		Pattern p_script = Pattern.compile(regEx_script, Pattern.CASE_INSENSITIVE);
		Matcher m_script = p_script.matcher(source);
		source = m_script.replaceAll(""); // 过滤script标签

		Pattern p_style = Pattern.compile(regEx_style, Pattern.CASE_INSENSITIVE);
		Matcher m_style = p_style.matcher(source);
		source = m_style.replaceAll(""); // 过滤style标签

		Pattern p_html = Pattern.compile(regEx_html, Pattern.CASE_INSENSITIVE);
		Matcher m_html = p_html.matcher(source);
		source = m_html.replaceAll(""); // 过滤html标签

		Pattern p_zhuanyi = Pattern.compile(regEx_zhuanyi, Pattern.CASE_INSENSITIVE);
		Matcher m_zhuanyi = p_zhuanyi.matcher(source);
		source = m_zhuanyi.replaceAll(""); // 过滤转义字符

		Pattern p_space = Pattern.compile(regEx_space, Pattern.CASE_INSENSITIVE);
		Matcher m_space = p_space.matcher(source);
		source = m_space.replaceAll("\n");

		return source.trim(); // 返回文本字符串
	}

	public static void highLight(JTextArea jTextArea, Vector<String> vector) {
		String text = jTextArea.getText();
		int pos = 0;
		Highlighter highlighter = jTextArea.getHighlighter();
		DefaultHighlightPainter painter = new DefaultHighlightPainter(Color.yellow);
		for (String string : vector) {
			while ((pos = text.indexOf(string, pos)) >= 0) {
				try {
					highlighter.addHighlight(pos, pos + string.length(), painter);
					pos += string.length();
				} catch (Exception e) {

				}
			}
		}
	}

	public static Vector<String> readWords(String fileName) throws Exception {
		Vector<String> vector = new Vector<>();
		BufferedReader bReader = new BufferedReader(
				new InputStreamReader(new FileInputStream(new File(fileName)), "GBK"));
		String line;
		while ((line = bReader.readLine()) != null) {
			vector.add(line);
		}
		return vector;
	}

	public static Vector<String> importLib(Vector<JButton> bVector, JLabel jLabel, normalFilter self) {
		Vector<String> vector = new Vector<>();
		JFileChooser jFileChooser = new JFileChooser();
		jFileChooser.setCurrentDirectory(new File("."));
		jFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		jFileChooser.setAcceptAllFileFilterUsed(false);
		jFileChooser.addChoosableFileFilter(new FileFilter() {

			@Override
			public String getDescription() {
				// TODO Auto-generated method stub
				return "文本文件(*.txt)";
			}

			@Override
			public boolean accept(File f) {
				// TODO Auto-generated method stub
				return f.getName().endsWith("txt") || f.isDirectory();
			}
		});
		jFileChooser.showDialog(new Label(), "选择");
		try {
			String fileName = jFileChooser.getSelectedFile().getAbsolutePath();
			vector = filter.readWords(fileName);
			jLabel.setText("已导入目标库：" + fileName);
			for (JButton jButton : bVector) {
				jButton.setEnabled(true);
			}
		} catch (Exception e2) {
			new JOptionPane().showMessageDialog(self, "目标库文件错误", "ERROR", JOptionPane.ERROR_MESSAGE);
			jLabel.setText("请导入库文件");
			for (JButton jButton : bVector) {
				jButton.setEnabled(false);
			}
		}
		return vector;
	}
}