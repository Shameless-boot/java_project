package com.game;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
public class GameFrame extends JFrame{
    private static final int DEFAULT_WIDTH = 410;
    private static final int DEFAULT_HEIGHT = 525;
    private JLabel title;
    private JLabel lsctip;
    private JLabel lgatip;
    private JLabel lscore;
    public GameFrame(){
        this.setLayout(null);
        MainJPanel mainJPanel = new MainJPanel();
        mainJPanel.setBounds(0,90,DEFAULT_WIDTH,DEFAULT_HEIGHT);
        add(mainJPanel, BorderLayout.SOUTH);
        pack();
        title = new JLabel("2048");
        title.setFont(new Font("", Font.BOLD, 50));
        title.setForeground(new Color(0x776e65));
        title.setBounds(5, 0, 120, 60);

        lsctip = new JLabel("SCORE", JLabel.CENTER);
        lsctip.setFont(new Font("", Font.BOLD, 18));
        lsctip.setForeground(new Color(0xeee4da));
        lsctip.setOpaque(true);
        lsctip.setBackground(new Color(0xbbada0));
        lsctip.setBounds(290, 5, 100, 25);

        lgatip = new JLabel("按方向键可以控制方块的移动，按ESC键可以重新开始游戏.", JLabel.CENTER);
        lgatip.setFont(new Font("", Font.ITALIC, 13));
        lgatip.setForeground(new Color(0x776e65));
        lgatip.setBounds(10, 65, 340, 15);

        lscore = new JLabel("0", JLabel.CENTER);
        lscore.setFont(new Font("", Font.BOLD, 25));
        lscore.setForeground(Color.WHITE);
        lscore.setOpaque(true);
        lscore.setBackground(new Color(0xbbada0));
        lscore.setBounds(290, 30, 100, 25);

        //设置背景颜色
        getContentPane().setBackground(new Color(0xfaf8ef));
        //设置窗体居中
        setLocationRelativeTo(getOwner());

        add(lscore);
        add(title);
        add(lgatip);
        add(lsctip);
    }
    public static void main(String[] args) {
        EventQueue.invokeLater(()->{
            GameFrame frame = new GameFrame();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setTitle("2048");
            frame.setResizable(false);//设置让窗口大小无法调节
            frame.setVisible(true);
        });
    }
    class MainJPanel extends JPanel{
        //墙砖
        private Tile[] tiles;
        private  final Color BG_COLOR = new Color(0xbbada0);
        private static final int GOAL = 2048;
        private static final int TILE_MARGIN = 16;
        private static final int TILE_SIZE = 80;
        private static final int DEFAULT_ROWS = 4;
        private static final int DEFAULT_COLUMNS = 4;
        private static final String FONT_NAME = "Arial";
        private boolean lose;
        private boolean win;
        private int score = 0;
        public MainJPanel(){
            setFocusable(true);
            //为游戏主面板添加按键监听器，监听键盘上的上、下、左、右
            addKeyListener(new KeyAdapter() {
                //实例化一个适配器类，不用于覆写KeyListener接口中所有方法,只覆写按下方法
                @Override
                public void keyPressed(KeyEvent e) {
                    super.keyPressed(e);
                    //如果用户按下esc，重置游戏
                    if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
                        resetGame();
                    if (isLose())
                        lose = true;
                    if (!win && !lose) {
                        switch (e.getKeyCode()) {
                            case KeyEvent.VK_LEFT:
                                left();
                                break;
                            case KeyEvent.VK_RIGHT:
                                right();
                                break;
                            case KeyEvent.VK_DOWN:
                                down();
                                break;
                            case KeyEvent.VK_UP:
                                up();
                                break;
                        }
                    }
                    repaint();
                }
            });
            resetGame();//开始游戏
        }
        private void left() {
            boolean needNewTile = false;
            //一行一行来进行合并处理
            for (int i = 0; i < DEFAULT_COLUMNS; i++) {
                Tile[] line = getLine(i);
                Tile[] newLine = mergeLine(moveLine(line));
                if (line != newLine)
                    setLine(i,newLine);
                if (!needNewTile && !compare(line,newLine))
                    needNewTile = true;
            }
            if (needNewTile)
                addTile();
        }
        //将矩阵左右翻转，把向右变成向左处理
        private void right() {
            leftSideRight();
            left();
            leftSideRight();
        }
        //将矩阵顺时针旋转90度，把列变成行进行左合并
        private void down() {
            rotate90cw();
            left();
            rotate90acw();
        }
        //同上
        private void up() {
            rotate90acw();
            left();
            rotate90cw();
        }
        /**
         *@Description: 将合并过的墙砖集合，赋给总的墙砖数组
         *@Params: y：行索引，newLine：进行过合并的墙砖集合
         *@return: void
         */
        private void setLine(int y, Tile[] newLine) {
            for (int x = 0; x < DEFAULT_ROWS; x++) {
                tiles[x + y * DEFAULT_ROWS] = newLine[x];
            }
        }
        /**
         *@Description: 用判断是否有合并过墙砖集合，如果合并则表示可以新添加一个墙砖
         *@Params: line1:未进行操作的墙砖集合，line2：进行过合并的墙砖集合
         *@return: 用于判断是否合并
         */
        private boolean compare(Tile[] line1,Tile[] line2) {
            if (line1 == line2) {
                return true;
            }
            for (int i = 0; i < line1.length; i++) {
                if (line1[i].value != line2[i].value) {
                    return false;
                }
            }
            return true;
        }
        /**
         *@Description: 判断传递进来的行中是否存在需要合并的墙砖，如果存在将其合并添加到新的行中，并将新行返回；
         *              如果不存在需要合并的，也将新行返回
         *@Params: 进行操作的行数据
         *@return: 新的行
         */
        private Tile[] mergeLine(Tile[] oldLine) {
            final int lineBorder = 3;
            List<Tile> newLine = new ArrayList<>();
            //因为新添加进来的行已经进行了整理，如果首个墙砖或某个墙砖的value值为空则表明后面已经没有需要比较的墙砖了
            for (int i = 0; i < DEFAULT_ROWS && oldLine[i].value != 0; i++) {
                int num = oldLine[i].value;
                if (i < lineBorder && oldLine[i].value == oldLine[i + 1].value){
                    num = oldLine[i].value * 2;
                    score += num;
                    i++;
                    if (num == GOAL)
                        win = true;
                }
                newLine.add(new Tile(num));
            }
            if (newLine.size() == 0)
                return oldLine;
            else{
                ensureSize(newLine);
                return newLine.toArray(new Tile[4]);
            }
        }
        /**
         *@Description: 将传递进来数组中的数值进行整理，将0值的排到该行的后面,方便合并
         *@Params: 传递一整行的数组
         *@return: 返回一个整理过的新数组，或者还是旧的数组
         */
        private Tile[] moveLine(Tile[] line) {
            LinkedList<Tile> newLine = new LinkedList<>();
            for (Tile tile : line) {
                if (tile.value != 0)
                    newLine.add(tile);
            }
            if (newLine.isEmpty())
                return line;

            if (newLine.size() < 4)
                ensureSize(newLine);
            Tile tile[] = new Tile[4];
            for (int i = 0; i < DEFAULT_ROWS; i++) {
                tile[i] = newLine.removeFirst();
            }
            return tile;
        }
        private void ensureSize(List<Tile> list) {
            while (list.size()!=4)
                list.add(new Tile());
        }
        /**
         *@Description: 根据指定行，获取一整行数组数据
         *@Params: 指定行值
         *@return: 一整行数组
         */
        private Tile[] getLine(int x) {
            Tile tile[] = new Tile[DEFAULT_ROWS];
            for (int y = 0; y < 4; y++) {
                tile[y] = tileAt(x,y);
            }
            return tile;
        }
        //将矩阵进行转置，再进行上下翻转，就可以实现逆时针旋转
        private void rotate90acw() {
            Tile[] tempTiles = initialTile();
            for (int x = 0; x < DEFAULT_ROWS; x++) {
                for (int y = 0; y < DEFAULT_COLUMNS; y++) {
                    tiles[y + x * 4] = tempTiles[y * DEFAULT_COLUMNS + x];
                }
            }
            upsideDown();
        }
        //同上
        private void rotate90cw() {
            upsideDown();
            Tile[] tempTiles = initialTile();
            for (int x = 0; x < DEFAULT_ROWS; x++) {
                for (int y = 0; y < DEFAULT_COLUMNS; y++) {
                    tiles[y + x * 4] = tempTiles[y * DEFAULT_COLUMNS + x];
                }
            }
        }
        //将矩阵进行左右翻转
        private void leftSideRight() {
            Tile[] tempTiles = initialTile();
            for (int x = 0; x < DEFAULT_ROWS; x++) {
                for (int y = 0; y < DEFAULT_COLUMNS; y++) {
                    tiles[y * DEFAULT_COLUMNS + x] = tempTiles[y * DEFAULT_COLUMNS + DEFAULT_COLUMNS - 1 - x];
                }
            }
        }
        //将矩阵进行上下翻转
        private void upsideDown() {
            Tile[] tempTiles = initialTile();
            for (int x = 0; x < DEFAULT_ROWS; x++) {
                for (int y = 0; y < DEFAULT_COLUMNS; y++) {
                    tiles[y + x * DEFAULT_ROWS] = tempTiles[y + (DEFAULT_ROWS - x - 1) * DEFAULT_ROWS];
                }
            }
        }
        private Tile[] initialTile(){
            Tile[] tempTile = new Tile[DEFAULT_COLUMNS * DEFAULT_ROWS];
            for (int x = 0; x < DEFAULT_ROWS; x++) {
                for (int y = 0; y < DEFAULT_COLUMNS; y++) {
                    tempTile[y + x * DEFAULT_ROWS] = tiles[y + x * DEFAULT_ROWS];
                }
            }
            return tempTile;
        }
        /**
         *@Description: 根据行和列的索引，获取指定的数据
         *@Params: y为行索引，x为列索引
         *@return: 返回指定的单个墙砖
         */
        private Tile tileAt(int x,int y){
            return tiles[y + x * 4];
        }
        private void resetGame() {
            tiles = new Tile[DEFAULT_ROWS * DEFAULT_COLUMNS];
            lose = false;
            win = false;
            //初始化全部墙砖
            for (int i = 0; i < tiles.length; i++)
                tiles[i] = new Tile();
            //随机生成两块墙砖
            addTile();
            addTile();
        }
        //添加一块新的墙砖
        private void addTile() {
            int value;
            List<Tile> list = emptyTile();
            int index = (int)(Math.random() * list.size()) % list.size();
            value = Math.random() > 9 ? 4 : 2;
            list.get(index).value = value;
        }
        //将为0的墙砖添加到集合中
        private List<Tile> emptyTile() {
            List<Tile> list = new ArrayList<>();
            for (Tile tile : tiles) {
                if (tile.value == 0)
                    list.add(tile);
            }
            return list;
        }
        @Override
        public void paint(Graphics g) {
            g.setColor(BG_COLOR);
            //绘画主面板颜色
            g.fillRect(0,0,this.getWidth(),this.getHeight());
            lscore.setText(String.valueOf(score));
            for (int y = 0; y < 4; y++)
                for (int x = 0; x < 4; x++)
                    drawTile(g,tiles[x + y * DEFAULT_ROWS],x,y);
        }
        private void drawTile(Graphics g2, Tile tile, int x, int y) {
            int value = tile.value;
            Graphics2D g2D = (Graphics2D) g2;
            g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2D.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);
            //根据当前墙砖的value值，获取背景颜色
            g2D.setColor(tile.getBackground());
            //获取每个矩形的左上角坐标
            int offsetX = offsetColorX(x);
            int offsetY = offsetColorY(y);
            //绘画圆角矩形：fillRoundRect()前两个为指定矩形左上角位置，中间两个为矩形的宽和高，最后两个为矩形圆角的圆角弧和直径
            g2D.fillRoundRect(offsetX,offsetY,TILE_SIZE,TILE_SIZE,16,16);
            Font font = new Font(FONT_NAME, Font.BOLD,tile.getFontSize());
            g2D.setFont(font);
            g2D.setColor(tile.getForeground());
            //g2D.drawString(String.valueOf(score),300,10);
            //获取字体的宽度和行间距
            String s = String.valueOf(value);
            final FontMetrics fm = getFontMetrics(font);
            final int w = fm.stringWidth(s);
            final int h = -(int) fm.getLineMetrics(s, g2D).getBaselineOffsets()[2];

            if (value != 0)
                g2D.drawString(s, offsetX + (TILE_SIZE - w) / 2, offsetY + TILE_SIZE - (TILE_SIZE - h) / 2 - 2);

            if (win || lose){
                score = 0;
                g2D.setColor(new Color(255,255,255,30));
                g2D.fillRect(0,0,this.getWidth(),this.getHeight());
                g2D.setColor(new Color(78, 139, 202));
                g2D.setFont(new Font(FONT_NAME, Font.BOLD, 48));
                if (lose){
                    g2D.drawString("Game Over!",70,160);
                    g2D.drawString("You Lose",90,230);
                }
                if (win){
                    g2D.drawString("You Win",105,200);
                }
            }
        }
        private boolean isLose(){
            for (int i = 0; i < DEFAULT_COLUMNS * DEFAULT_ROWS; i++)
                if (tiles[i].value == 0)
                    return false;

            for (int x = 0; x < DEFAULT_ROWS - 1; x++) {
                for (int y = 0; y < DEFAULT_COLUMNS - 1; y++) {
                    Tile tile = tileAt(x, y);
                    if (tile.value == tileAt(x + 1, y).value ||
                        tile.value == tileAt(x, y + 1).value)
                        return false;
                }
            }
            return true;
        }

        //根据给定的x值，求出每个矩形左上角的x坐标值
        private int offsetColorX(int x) {
            return x * (TILE_MARGIN + TILE_SIZE) + TILE_MARGIN;
        }
        //根据给定的y值，求出每个矩形左上角的y坐标值
        private int offsetColorY(int y) {
            return y * (TILE_MARGIN + TILE_SIZE) + TILE_MARGIN;
        }
        class Tile{
            int value;
            public Tile(int value) {
                this.value = value;
            }
            public Tile(){
                this(0);
            }
            private int getFontSize(){
                return this.value < 100 ? 36 : this.value < 1000 ? 32 : 24;
            }
            private Color getForeground(){
                return value < 16 ? new Color(0x776e65) :  new Color(0xf9f6f2);
            }
            private Color getBackground(){
                switch (this.value){
                    case 2:    return new Color(0xeee4da);
                    case 4:    return new Color(0xede0c8);
                    case 8:    return new Color(0xf2b179);
                    case 16:   return new Color(0xf59563);
                    case 32:   return new Color(0xf67c5f);
                    case 64:   return new Color(0xf65e3b);
                    case 128:  return new Color(0xedcf72);
                    case 256:  return new Color(0xedcc61);
                    case 512:  return new Color(0xedc850);
                    case 1024: return new Color(0xedc53f);
                    case 2048: return new Color(0xedc22e);
                    default:return new Color(0xcdc1b4);
                }
            }
        }
    }
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }
}