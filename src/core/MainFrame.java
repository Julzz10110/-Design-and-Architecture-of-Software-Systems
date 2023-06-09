package core;

import charts.BarChart;
import charts.Chart;
import core.design.GUIDesignerFrame;
import core.state.ChangedState;
import core.state.OriginalState;
import core.state.ResetState;
import core.state.StateContext;
import data.DataMediator;
import data.strategy.*;
import tools.ChartSnapshotManager;
import tools.FileManager;
import tools.ImageConverter;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Stack;

public class MainFrame extends JFrame {
    public static final int MAIN_FRAME_WIDTH = 1600;
    public static final int MAIN_FRAME_HEIGHT = 800;
    private static ChartSnapshotManager snapshotManager = new ChartSnapshotManager();
    private Color backgroundColor;
    private Color textColor;
    private static final int ICON_WIDTH = 20;
    private static final int ICON_HEIGHT = 20;
    private JFrame frame;
    private JMenuBar menuBar;
    private JLabel loadedDataLabel;
    private JLabel statInfoLabel;
    private Box contents;
    private Font textFont;
    private ArrayList<JLabel> labelsHeap;
    private JPanel dataPanel;
    private JPanel tablePanel;
    private JPanel chartPanel;
    private ArrayList<JButton> buttonsHeap;
    private JButton commitChangesButton;
    private JButton resetChangesButton;
    private JButton buildChartButton;
    private JButton snapshotButton;
    private JMenu settings;
    private JPanel buttonsPanel;
    private JMenuItem settingShapshotPath;
    private JFrame shapshotPathFrame;
    private JButton setDirectoryButton;
    private JMenu file;
    private JMenuItem open;
    private static Chart chart = new BarChart("", false);
    private static ResourceBundle rb;

    private static JPanel canvasPanel;
    private JMenuItem convert;
    private static JTable inputTable;
    private JMenuItem design;
    private Stack<JFrame> childFrameStack;

    private MainFrame() {
        Locale locale = new Locale("ru", "RU");
        rb = ResourceBundle.getBundle("core/i18n/Resources", locale);
        create();
        labelsHeap = new ArrayList<>();
        buttonsHeap = new ArrayList<>();
        backgroundColor = getBackground();
        textColor = getForeground();
        textFont = getFont();
        translateFileChooserText();
        GridLayout mainLayout = new GridLayout(0, 2);
        setLayout(mainLayout);
        dataPanel = new JPanel();
        loadedDataLabel = new JLabel(rb.getString("downloaded_data"));
        labelsHeap.add(loadedDataLabel);
        buttonsPanel = new JPanel();
        FlowLayout buttonsLayout = new FlowLayout();
        buttonsPanel.setLayout(buttonsLayout);
        commitChangesButton = new JButton(rb.getString("commit_changes"));
        buttonsHeap.add(commitChangesButton);
        commitChangesButton.setEnabled(false);
        resetChangesButton = new JButton(rb.getString("reset_changes"));
        buttonsHeap.add(resetChangesButton);
        resetChangesButton.setEnabled(false);
        buttonsPanel.add(commitChangesButton);
        buttonsPanel.add(resetChangesButton);
        dataPanel.add(buttonsPanel);
        dataPanel.add(loadedDataLabel);
        contents = new Box(BoxLayout.Y_AXIS);

        tableModelContext = new StateContext();

        inputTable = new JTable();

        //tableModelContext.setState(new ResetState());
        contents.add(new JScrollPane(inputTable));
        tablePanel = new JPanel();
        tablePanel.add(contents);
        dataPanel.add(tablePanel);
        dataPanel.add(buttonsPanel);
        tablePanel.repaint();
        tablePanel.revalidate();

        statInfoLabel = new JLabel(rb.getString("stat_info"));
        labelsHeap.add(statInfoLabel);
        statInfoLabel.setForeground(textColor);
        loadedDataLabel.add(statInfoLabel);
        statInfoLabel.setVisible(false);
        dataPanel.add(statInfoLabel);

        chartPanel = new JPanel();
        dataPanel.setPreferredSize(new Dimension((int) (0.3 * MAIN_FRAME_WIDTH), MAIN_FRAME_HEIGHT));
        dataPanel.setLayout(new BoxLayout(dataPanel, BoxLayout.Y_AXIS));
        add(dataPanel);
        add(chartPanel);


        mediator = DataMediator.getInstance();
        mediator.setMainFrame(this);
        mediator.setFileManager(FileManager.getInstance());

        canvasPanel = new JPanel();
        canvasPanel.add(chart.getChartPanel());
        chartPanel.add(canvasPanel, BorderLayout.CENTER);
        //chartPanel.setSize(chartPanel.getWidth(), chartPanel.getHeight());
        chartPanel.validate();

        buildChartButton = new JButton(rb.getString("build_chart"));
        buttonsHeap.add(buildChartButton);
        buildChartButton.setBackground(new Color(0x7C7CE3));
        chartPanel.add(buildChartButton, BorderLayout.SOUTH);

        snapshotButton = new JButton(rb.getString("save_png"));
        buttonsHeap.add(snapshotButton);
        snapshotButton.setBackground(new Color(0xFF2424));
        snapshotButton.setEnabled(snapshotManager.isEnabled());
        chartPanel.add(snapshotButton, BorderLayout.SOUTH);

        savingDirectoryPathLabel = new JLabel();
        savingDirectoryPathLabel.setText(rb.getString("snapshots_path") + snapshotManager.getSnapshotsPath());
        labelsHeap.add(savingDirectoryPathLabel);
        chartPanel.add(savingDirectoryPathLabel);


        commitChangesButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                tableModelContext.setState(new ChangedState());
                tableModelContext.updateDataFrame();
            }
        });


        resetChangesButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                tableModelContext.setState(new ResetState());
                tableModelContext.updateDataFrame();
            }
        });


        snapshotButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    snapshotManager.getSaveSnapshot(chart.getChartPanel(), snapshotManager.getSnapshotsPath() + "/" + LocalDateTime.now());
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        });

        buildChartButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ChartSettingsFrame chartSettingsFrame = new ProxyChartSettingsFrame(mediator.sendDataFrame());
                chartSettingsFrame.displayFrame();
                childFrameStack.push((JFrame) ((ProxyChartSettingsFrame) chartSettingsFrame).getChartSettingsFrame());
                snapshotButton.setEnabled(snapshotManager.isEnabled());
            }
        });

    }

    public static MainFrame getInstance() {
        return MainFrameHolder.instance;
    }

    public static ChartSnapshotManager getSnapshotManager() {
        return snapshotManager;
    }

    public static void main(String[] args) {
        MainFrame mainFrame = MainFrame.getInstance();
        mainFrame.repaint();
        mainFrame.revalidate();
    }

    private static StateContext tableModelContext;
    private JMenu language;

    public Color getBackgroundColor() {
        return backgroundColor;
    }
    private JTable statTable = new JTable();

    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public Color getTextColor() {
        if (textColor != null) return textColor;
        else return Color.black;
    }

    public void setTextColor(Color textColor) {
        this.textColor = textColor;
    }

    public static JTable getInputTable() {
        return inputTable;
    }

    public Font getTextFont() {
        return textFont;
    }

    private JLabel savingDirectoryPathLabel;

    public static StateContext getTableModelContext() {
        return tableModelContext;
    }

    public static JPanel getCanvasPanel() {
        return canvasPanel;
    }

    public void setTextFont(Font textFont) {
        this.textFont = textFont;
    }


    private DataMediator mediator;

    public static Chart getChart() {
        return chart;
    }

    public static void setChart(Chart chart) {
        MainFrame.chart = chart;
    }

    private JMenu createSettingsMenu()
    {
        settings = new JMenu(rb.getString("settings"));
        Image image = null;
        try {
            image = ImageIO.read(getClass().getResource("/res/roller.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        design = new JMenuItem(rb.getString("set_appearance"),
                new ImageIcon(image.getScaledInstance(ICON_WIDTH, ICON_HEIGHT, Image.SCALE_DEFAULT)));

        try {
            image = ImageIO.read(getClass().getResource("/res/save.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        settingShapshotPath = new JMenuItem(rb.getString("choose_chart_directory"),
                new ImageIcon(image.getScaledInstance(ICON_WIDTH, ICON_HEIGHT, Image.SCALE_DEFAULT)));

        settings.add(design);
        settings.addSeparator();
        settings.add(settingShapshotPath);


        design.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                GUIDesignerFrame guiDesignerFrame = new GUIDesignerFrame(MainFrame.getInstance());
                childFrameStack.push((JFrame) guiDesignerFrame);
            }
        });


        settingShapshotPath.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                shapshotPathFrame = new JFrame();
                int frameWidth = (int) (0.5 * MAIN_FRAME_WIDTH);
                int frameHeight = (int) (0.2 * MAIN_FRAME_HEIGHT);
                childFrameStack.push(shapshotPathFrame);
                shapshotPathFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                shapshotPathFrame.setTitle(rb.getString("choose_save_directory"));
                shapshotPathFrame.setSize((int) (0.5 * MAIN_FRAME_WIDTH), (int) (0.2 * MAIN_FRAME_HEIGHT));
                shapshotPathFrame.setLocationRelativeTo(null);
                shapshotPathFrame.getContentPane().setBackground(backgroundColor);
                shapshotPathFrame.setResizable(true);
                shapshotPathFrame.setVisible(true);
                shapshotPathFrame.setLayout(new FlowLayout());

                TextField directoryPathField = new TextField();
                directoryPathField.setPreferredSize(new Dimension((int)(0.7 * frameWidth), (int)(0.2 * frameHeight)));
                shapshotPathFrame.add(directoryPathField);

                setDirectoryButton = new JButton(rb.getString("open"));
                setDirectoryButton.setBackground(new Color(0x7C7CE3));
                setDirectoryButton.setFont(textFont);
                shapshotPathFrame.add(setDirectoryButton);

                JButton okButton = new JButton(rb.getString("ok"));
                okButton.setBackground(new Color(0xD9CFCF));
                okButton.setFont(textFont);
                shapshotPathFrame.add(okButton);

                final String[] directoryPath = new String[1];
                setDirectoryButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        JFileChooser fileChooser = new JFileChooser();
                        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                        int option = fileChooser.showOpenDialog(frame);
                        if (option == JFileChooser.APPROVE_OPTION) {
                            directoryPath[0] = fileChooser.getSelectedFile().getPath();
                            directoryPathField.setText(directoryPath[0]);
                            if (directoryPath[0] == null) directoryPathField.setText(ChartSnapshotManager.DEFAULT_SNAPSHOTS_PATH);
                        }
                    }
                });

                okButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        snapshotManager.setSnapshotsPath(directoryPath[0]);
                        if (directoryPath[0] == null)
                            snapshotManager.setSnapshotsPath(ChartSnapshotManager.DEFAULT_SNAPSHOTS_PATH);
                        System.out.println(rb.getString("save_chart_directory") + snapshotManager.getSnapshotsPath());
                        savingDirectoryPathLabel.setText(rb.getString("snapshots_path") + snapshotManager.getSnapshotsPath());
                        shapshotPathFrame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));

                    }
                });
            }
        });
        return settings;
    }

    private JMenu createFileMenu() {
        // Создание выпадающего меню
        file = new JMenu(rb.getString("file"));
        // Пункт меню "Открыть" с изображением
        Image image = null;
        try {
            image = ImageIO.read(getClass().getResource("/res/open.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        open = new JMenuItem(rb.getString("open"),
                new ImageIcon(image.getScaledInstance(ICON_WIDTH, ICON_HEIGHT, Image.SCALE_DEFAULT)));
        // Пункт меню из команды с выходом из программы
        // Добавление к пункту меню изображения
        // Добавим в меню пункта open
        file.add(open);
        // Добавление разделителя
        file.addSeparator();

        try {
            image = ImageIO.read(getClass().getResource("/res/convert.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        convert = new JMenuItem(rb.getString("convert_image"),
                new ImageIcon(image.getScaledInstance(ICON_WIDTH, ICON_HEIGHT, Image.SCALE_DEFAULT)));
        file.add(convert);

        open.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                JFileChooser fileChooser = new JFileChooser();
                UIManager.getLookAndFeelDefaults().put("FileChooser.background", backgroundColor);
                UIManager.getLookAndFeelDefaults().put("FileChooser[Enabled].backgroundPainter",
                        new Painter<JFileChooser>() {
                            @Override
                            public void paint(Graphics2D g, JFileChooser object, int width, int height) {
                                g.setColor(backgroundColor);
                                g.draw(object.getBounds());

                            }
                        });


                UIManager.getLookAndFeelDefaults().put("FileChooser.foreground", textColor);
                int option = fileChooser.showOpenDialog(frame);
                if (option == JFileChooser.APPROVE_OPTION) {

                    File file = fileChooser.getSelectedFile();
                    int dotIndex = file.getAbsolutePath().lastIndexOf('.');
                    String extension = (dotIndex == -1) ? "" : file.getAbsolutePath().substring(dotIndex + 1);
                    if (extension.equals("xlsx") || extension.equals("xls")) {
                        mediator.getFileManager().loadDataExcel(file.getPath());
                        tableModelContext.setState(new OriginalState());
                        tableModelContext.updateDataFrame();
                    } else if (extension.equals("csv")) {
                        mediator.getFileManager().loadDataCSV(file.getPath());
                        tableModelContext.setState(new OriginalState());
                        tableModelContext.updateDataFrame();
                    } else {
                        System.out.println("ОШИБКА: Невозможно открыть файл");
                        return;
                    }
                    DefaultTableModel inputTableModel = mediator.sendTabelModel();
                    //tableModelContext.setState(new ResetState());
                    inputTable.setModel(inputTableModel);

                    Action action = new AbstractAction() {
                        public void actionPerformed(ActionEvent e) {
                            TableCellListener tcl = (TableCellListener) e.getSource();
                            //System.out.println("Row   : " + tcl.getRow());
                            //System.out.println("Column: " + tcl.getColumn());
                            //System.out.println("Old   : " + tcl.getOldValue());
                            //System.out.println("New   : " + tcl.getNewValue());
                            ArrayList<Object> change = new ArrayList<>();
                            change.add(inputTable.getColumnName(tcl.getColumn()));
                            change.add(tcl.getColumn());
                            change.add(tcl.getRow());
                            change.add(tcl.getOldValue());
                            change.add(tcl.getNewValue());
                            //System.out.println(Arrays.toString(change.toArray()));
                            mediator.getInputTableChanges().add(change);
                            //tableModelContext.setState(new ChangedState());
                        }
                    };
                    TableCellListener tcl = new TableCellListener(inputTable, action);

                    contents = new Box(BoxLayout.Y_AXIS);
                    contents.add(new JScrollPane(inputTable));
                    tablePanel.add(contents);
                    tablePanel.repaint();
                    tablePanel.revalidate();

                    String[] keySetArray = new String[mediator.sendDataFrame().getData().keySet().toArray().length + 1];
                    keySetArray[0] = rb.getString("value_name");
                    for (int i = 1; i < keySetArray.length; i++) {
                        keySetArray[i] = mediator.sendDataFrame().getData().keySet().toArray()[i - 1].toString();

                    }

                    DefaultTableModel statTableModel = new DefaultTableModel(keySetArray, 0);
                    String[] statFunctions = new String[]{"count", "mean", "std", "min", "max", "1st quantile", "median", "3rd quantile"};
                    ArrayList<String[]> statData = new ArrayList<>();
                    statData.add(statFunctions);
                    for (String key : mediator.sendDataFrame().getData().keySet()) {
                        String[] columnStatData = new String[statFunctions.length];

                        StatCalculationContext context = new StatCalculationContext();

                        context.setStrategy(new ConcreteStrategyCount());
                        columnStatData[0] = context.executeStrategy(mediator.sendDataFrame(), key).toString();

                        context.setStrategy(new ConcreteStrategyMean());
                        columnStatData[1] = context.executeStrategy(mediator.sendDataFrame(), key).toString();

                        context.setStrategy(new ConcreteStrategyStd());
                        columnStatData[2] = context.executeStrategy(mediator.sendDataFrame(), key).toString();

                        context.setStrategy(new ConcreteStrategyMin());
                        columnStatData[3] = context.executeStrategy(mediator.sendDataFrame(), key).toString();

                        context.setStrategy(new ConcreteStrategyMax());
                        columnStatData[4] = context.executeStrategy(mediator.sendDataFrame(), key).toString();

                        context.setStrategy(new ConcreteStrategyPercentile());
                        columnStatData[5] = context.executeStrategy(mediator.sendDataFrame(), key, 0.25).toString();
                        columnStatData[6] = context.executeStrategy(mediator.sendDataFrame(), key, 0.5).toString();
                        columnStatData[7] = context.executeStrategy(mediator.sendDataFrame(), key, 0.75).toString();

                        statData.add(columnStatData);

                        //System.out.println(Arrays.toString(columnStatData));
                    }

                    String[] rowStatData = new String[statFunctions.length];
                    int index = 0;
                    for (int i = 0; i < rowStatData.length; i++) {
                        int j = 0;
                        for (int k = 0; k < statData.size(); k++) {
                            rowStatData[j] = statData.get(k)[index];
                            j++;
                        }
                        statTableModel.addRow(rowStatData);
                        index++;
                    }
                    statInfoLabel.setVisible(true);
                    statTable.setModel(statTableModel);
                    Box contents1 = new Box(BoxLayout.Y_AXIS);
                    contents1.add(new JScrollPane(statTable));
                    dataPanel.add(contents1);
                    dataPanel.repaint();
                    dataPanel.revalidate();
                    commitChangesButton.setEnabled(true);
                    resetChangesButton.setEnabled(true);

                }else{
                    System.out.println("Open command canceled");
                }
            }
        });

        convert.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                JFileChooser fileChooser = new JFileChooser(snapshotManager.getSnapshotsPath());
                int option = fileChooser.showOpenDialog(frame);
                if (option == JFileChooser.APPROVE_OPTION) {
                    File file = fileChooser.getSelectedFile();
                    JFrame chooseFormatsFrame = new JFrame();
                    chooseFormatsFrame.getContentPane().setBackground(backgroundColor);
                    int frameWidth = (int) (0.5 * MAIN_FRAME_WIDTH);
                    int frameHeight = (int) (0.2 * MAIN_FRAME_HEIGHT);
                    chooseFormatsFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                    chooseFormatsFrame.setTitle(rb.getString("choose_save_directory"));
                    chooseFormatsFrame.setSize((int) (0.5 * MAIN_FRAME_WIDTH), (int) (0.2 * MAIN_FRAME_HEIGHT));
                    chooseFormatsFrame.setLocationRelativeTo(null);
                    chooseFormatsFrame.setResizable(true);
                    chooseFormatsFrame.setVisible(true);
                    chooseFormatsFrame.setLayout(new GridLayout(2, 0));
                    JLabel chooseFormatLabel = new JLabel(rb.getString("choose_image_formats"));
                    chooseFormatLabel.setFont(textFont);

                    chooseFormatsFrame.add(chooseFormatLabel);
                    JPanel boxesPanel = new JPanel();
                    boxesPanel.setLayout(new FlowLayout());

                    JCheckBox pngBox = new JCheckBox(".png");
                    JCheckBox jpegBox = new JCheckBox(".jpeg");
                    JCheckBox bmpBox = new JCheckBox(".bmp");
                    JCheckBox gifBox = new JCheckBox(".gif");
                    JCheckBox tiffBox = new JCheckBox(".tiff");

                    boxesPanel.add(pngBox);
                    boxesPanel.add(jpegBox);
                    boxesPanel.add(bmpBox);
                    boxesPanel.add(gifBox);
                    boxesPanel.add(tiffBox);

                    boxesPanel.setBackground(backgroundColor);

                    pngBox.setBackground(backgroundColor);
                    pngBox.setForeground(textColor);
                    pngBox.setFont(textFont);
                    jpegBox.setBackground(backgroundColor);
                    jpegBox.setForeground(textColor);
                    jpegBox.setFont(textFont);
                    bmpBox.setBackground(backgroundColor);
                    bmpBox.setForeground(textColor);
                    bmpBox.setFont(textFont);
                    gifBox.setBackground(backgroundColor);
                    gifBox.setForeground(textColor);
                    gifBox.setFont(textFont);
                    tiffBox.setBackground(backgroundColor);
                    tiffBox.setForeground(textColor);
                    tiffBox.setFont(textFont);

                    JButton okButton = new JButton(rb.getString("ok"));
                    okButton.setBackground(new Color(0x7C7CE3));
                    okButton.setFont(textFont);
                    boxesPanel.add(okButton);

                    chooseFormatsFrame.add(boxesPanel);

                    ArrayList<String> selectedFormats = new ArrayList<>();

                    pngBox.addItemListener(new ItemListener() {
                        public void itemStateChanged(ItemEvent e) {
                            if (!selectedFormats.contains("png")) selectedFormats.add("png");
                            else selectedFormats.remove("png");
                        }
                    });

                    jpegBox.addItemListener(new ItemListener() {
                        public void itemStateChanged(ItemEvent e) {
                            if (!selectedFormats.contains("jpeg")) selectedFormats.add("jpeg");
                            else selectedFormats.remove("jpeg");
                        }
                    });

                    bmpBox.addItemListener(new ItemListener() {
                        public void itemStateChanged(ItemEvent e) {
                            if (!selectedFormats.contains("bmp")) selectedFormats.add("bmp");
                            else selectedFormats.remove("bmp");
                        }
                    });

                    gifBox.addItemListener(new ItemListener() {
                        public void itemStateChanged(ItemEvent e) {
                            if (!selectedFormats.contains("gif")) selectedFormats.add("gif");
                            else selectedFormats.remove("gif");
                        }
                    });

                    tiffBox.addItemListener(new ItemListener() {
                        public void itemStateChanged(ItemEvent e) {
                            if (!selectedFormats.contains("tiff")) selectedFormats.add("tiff");
                            else selectedFormats.remove("tiff");
                        }
                    });

                    okButton.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            for (String format : selectedFormats) ImageConverter.convert(file.getAbsolutePath(), format);
                            chooseFormatsFrame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
                        }
                    });
                }
            }
        });

        return file;
    }


    private JMenu createLanguageMenu() {

        language = new JMenu(rb.getString("language"));
        Image image = null;
        try {
            image = ImageIO.read(getClass().getResource("/res/language/ru.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        JMenuItem ru = new JMenuItem("Русский",
                new ImageIcon(image.getScaledInstance(ICON_WIDTH, ICON_HEIGHT, Image.SCALE_DEFAULT)));

        try {
            image = ImageIO.read(getClass().getResource("/res/language/en.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        JMenuItem en = new JMenuItem("English",
                new ImageIcon(image.getScaledInstance(ICON_WIDTH, ICON_HEIGHT, Image.SCALE_DEFAULT)));

        try {
            image = ImageIO.read(getClass().getResource("/res/language/cn.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        JMenuItem cn = new JMenuItem("中文",
                new ImageIcon(image.getScaledInstance(ICON_WIDTH, ICON_HEIGHT, Image.SCALE_DEFAULT)));

        try {
            image = ImageIO.read(getClass().getResource("/res/language/de.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        JMenuItem de = new JMenuItem("Deutsch",
                new ImageIcon(image.getScaledInstance(ICON_WIDTH, ICON_HEIGHT, Image.SCALE_DEFAULT)));

        try {
            image = ImageIO.read(getClass().getResource("/res/language/fr.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        JMenuItem fr = new JMenuItem("Français",
                new ImageIcon(image.getScaledInstance(ICON_WIDTH, ICON_HEIGHT, Image.SCALE_DEFAULT)));

        language.add(ru);
        language.add(en);
        language.add(cn);
        language.add(de);
        language.add(fr);

        ru.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Locale locale = Locale.getDefault();
                rb = ResourceBundle.getBundle("core/i18n/Resources", locale);
                updateLanguage();
            }
        });

        en.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ResourceBundle.clearCache();
                Locale locale = new Locale("en", "UK");
                rb = ResourceBundle.getBundle("core/i18n/Resources", locale);
                updateLanguage();
            }
        });

        cn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ResourceBundle.clearCache();
                Locale locale = new Locale("zh", "CN");
                rb = ResourceBundle.getBundle("core/i18n/Resources", locale);
                updateLanguage();
            }
        });

        de.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ResourceBundle.clearCache();
                Locale locale = new Locale("de", "DE");
                rb = ResourceBundle.getBundle("core/i18n/Resources", locale);
                updateLanguage();
            }
        });

        fr.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ResourceBundle.clearCache();
                Locale locale = new Locale("fr", "FR");
                rb = ResourceBundle.getBundle("core/i18n/Resources", locale);
                updateLanguage();
            }
        });


        return language;
    }

    public static ResourceBundle getResourceBundle() {
        return rb;
    }

    private void updateLanguage() {
        setTitle(rb.getString("program_name"));
        loadedDataLabel.setText(rb.getString("downloaded_data"));
        commitChangesButton.setText(rb.getString("commit_changes"));
        resetChangesButton.setText(rb.getString("reset_changes"));
        statInfoLabel.setText(rb.getString("stat_info"));
        buildChartButton.setText(rb.getString("build_chart"));
        snapshotButton.setText(rb.getString("save_png"));
        savingDirectoryPathLabel.setText(rb.getString("snapshots_path") + snapshotManager.getSnapshotsPath());
        settings.setText(rb.getString("settings"));
        design.setText(rb.getString("set_appearance"));
        settingShapshotPath.setText(rb.getString("choose_chart_directory"));
        file.setText(rb.getString("file"));
        open.setText(rb.getString("open"));
        convert.setText(rb.getString("convert_image"));
        language.setText(rb.getString("language"));
        translateFileChooserText();
    }

    public void updateDesign() {
        dataPanel.setBackground(backgroundColor);
        chartPanel.setBackground(backgroundColor);
        tablePanel.setBackground(backgroundColor);
        buttonsPanel.setBackground(backgroundColor);

        for (JLabel label : labelsHeap) {
            label.setForeground(textColor);
            label.setFont(textFont);
        }

        for (JButton button : buttonsHeap) {
            button.setFont(textFont);
        }


    }


    public void create() {
        frame = new JFrame();
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setTitle(rb.getString("program_name"));
        setSize(MAIN_FRAME_WIDTH, MAIN_FRAME_HEIGHT);
        setLocationRelativeTo(null);
        setResizable(true);
        setVisible(true);

        menuBar = new JMenuBar();
        menuBar.add(createFileMenu());
        menuBar.add(createSettingsMenu());
        menuBar.add(Box.createHorizontalGlue());
        menuBar.add(createLanguageMenu());
        this.setJMenuBar(menuBar);
        childFrameStack = new Stack<>();
        this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                while (!childFrameStack.empty()) {
                    JFrame childFrame = childFrameStack.pop();
                    if (childFrame != null) childFrame.dispose();
                }
                System.exit(0);
            }
        });
    }

    private void translateFileChooserText() {
        UIManager.put("FileChooser.openDialogTitleText", rb.getString("open"));
        UIManager.put("FileChooser.lookInLabelText", rb.getString("look_in"));
        UIManager.put("FileChooser.fileNameLabelText", rb.getString("file_name"));
        UIManager.put("FileChooser.filesOfTypeLabelText", rb.getString("files_of_type"));
        UIManager.put("FileChooser.openButtonText", rb.getString("open"));
        UIManager.put("FileChooser.cancelButtonText", rb.getString("cancel"));
        UIManager.put("FileChooser.acceptAllFileFilterText", rb.getString("all_files"));

        UIManager.put("FileChooser.saveDialogTitleText", rb.getString("save_as"));
        UIManager.put("FileChooser.saveInLabelText", rb.getString("save_in"));
        UIManager.put("FileChooser.saveButtonText", rb.getString("save"));
    }

    private static class MainFrameHolder {
        private final static MainFrame instance = new MainFrame();
    }
}
