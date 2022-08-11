import javax.swing.*;
import java.awt.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage ;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener ;


public class Game extends JPanel implements Runnable, KeyListener {    // Orga des composants

    Frog frog;
    Lane[] lanes ;
    Winner[] winners;
    Loser[] losers;


    boolean isRunning ;
    Thread thread ;
    BufferedImage view, background;

    public final int WIDTH = 450 ;
    public final int HEIGHT = (int) (WIDTH / 1.14) ;
    public final int SCALE = 2;

    public Game ()  {
        setPreferredSize(new Dimension (WIDTH * SCALE, HEIGHT * SCALE));
        addKeyListener (this);
    }

    public static void main (String[] args) {
        JFrame w = new JFrame("Frogger");
        w.setResizable(false);
        w.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Quand on est sur le close bouton, on close
        w.add (new Game ());
        w.pack ();  // ajuste directement la taille du cadre
        w.setLocationRelativeTo(null);
        w.setVisible(true);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (thread == null) {
            thread = new Thread (this);
            isRunning = true;
            thread.start();
        }
    }

    /**
     * Initialisation de tous les composants du jeu
     */
    public void start () {
        try {
            view = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
            background = ImageIO.read(getClass().getResource("/background.png"));
            frog = new Frog ((int) (WIDTH / 2) , HEIGHT, 27, 20);

            lanes = new Lane[11]; // obstacles sur les voies
            lanes[0] = new Lane(TypeObstacle.CAR_1, 8, 3, -1f, 160);
            lanes[1] = new Lane(TypeObstacle.CAR_4, 9, 4, -0.7f, 130);
            lanes[2] = new Lane(TypeObstacle.CAR_3, 10, 3, -1.4f, 140);
            lanes[3] = new Lane(TypeObstacle.CAR_2, 11, 2, -2f, 200);
            lanes[4] = new Lane(TypeObstacle.CAR_5, 12, 2, -1f, 150);

            lanes[5] = new Lane(TypeObstacle.LOG_1, 2, 2, -1f, 250);
            lanes[6] = new Lane(TypeObstacle.LOG_4, 3, 2, 1f, 200);
            lanes[7] = new Lane(TypeObstacle.LOG_3, 4, 1, -2f, 130);
            lanes[8] = new Lane(TypeObstacle.LOG_2, 5, 2, 1.4f, 300);
            lanes[9] = new Lane(TypeObstacle.LOG_5, 6, 3, -0.8f, 150);

            lanes[10] = new Lane(TypeObstacle.SNAKE, 7, 1, 0.8f, 150);

            winners = new Winner[10];  // Emplacement de la victoire
            for (int i = 0; i<5 ; i++) {
                winners[2*i] = new Winner ((3* i * 28) + 28, 28);
                winners[2*i +1] = new Winner (((3*i +1) * 28) + 28 , 28);
            }

            losers = new Loser[6];  // de la défaite après traversée des voies
            for (int i = 0; i<6; i++) {
                losers[i] = new Loser ( (i*84), 28);
            }

        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void update () {
        frog.update();

        for (Lane lane : lanes) {
            lane.update();
        }

        int laneIndex = (int) (frog.y / 28);  // voie sur laquelle se trouve la grenouille
       // System.out.print (laneIndex);

        if (laneIndex == 7) {  //  on est sur la voie du snake
            lanes[10].check(frog);
        } else if (laneIndex >= 8 && laneIndex <= 12) { // voies des voitures
            laneIndex -= 8;
            lanes[laneIndex].check(frog);
        } else if (laneIndex >=2 && laneIndex <= 6) { // voies des rondins
         laneIndex += 3;
         lanes[laneIndex].check(frog);
         }

        for (Winner winner : winners) {
            if (frog.intersects(winner)) {
                if (!winner.visible) {
                    winner.visible = true;  // Un des points de victoire à été atteint
                } else {
                    frog.resetFrog();
                }
            }
        }

        for (Loser loser : losers) {
            if (frog.intersects(loser)) {
                loser.falling = true;  // on a traversé les voies mais est tombé dans un trou noir
                    frog.resetFrog();
            }
            loser.update();
        }

    }

    public void draw () {
        Graphics2D g2 = (Graphics2D) view.getGraphics();
        g2.drawImage (background, 0, 0, WIDTH, HEIGHT, null);

        for (Lane lane : lanes) {
            lane.draw(g2);
        }

        for (Winner winner : winners) {
            winner.draw(g2);
        }

        for (Loser loser : losers) {
            loser.draw(g2);
        }

        frog.draw(g2);

        Graphics g = getGraphics ();
        g.drawImage(view, 0, 0, WIDTH * SCALE , HEIGHT * SCALE, null);
    }

    @Override
    public void run () {
        try {
            requestFocus();
            start ();
            while (isRunning) {
                update ();
                draw();
                Thread.sleep (1000/60);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void keyTyped (KeyEvent e) {
    }

    @Override
    public void keyPressed (KeyEvent e) {
        frog.keyPressed(e);
    }

    @Override
    public void keyReleased (KeyEvent e) {
    }

    enum TypeObstacle {
        CAR_1, CAR_2,CAR_3,CAR_4,CAR_5, LOG_1, LOG_2,LOG_3,LOG_4,LOG_5, SNAKE
    }

    public class Obstacle extends BoxCollider {
        float speed = 0;
        BufferedImage obstacle;
        BufferedImage[] anim;
        TypeObstacle type;
        int frameIndex;

        public Obstacle(TypeObstacle type, float x, float y, float width, float height, float speed) {

            super(x, y, width, height);

            try {
                this.speed = speed ;
                this.type = type ;
                BufferedImage spriteSheets = ImageIO.read (getClass().getResource ("/cars_sprite_sheet_00.png"));
                BufferedImage spriteSheets2 = ImageIO.read (getClass().getResource ("/log_sprite_sheet_00.png"));
                BufferedImage spriteSheets3 = ImageIO.read (getClass().getResource ("/snake_sprite_sheet.png"));

                int carTileSize = 128;
                int carTileSizey = 76;

                 switch (type) {
                     case CAR_1 :
                         obstacle = spriteSheets.getSubimage (0,0,carTileSize, carTileSizey);
                         break;
                     case CAR_2 :
                         obstacle = spriteSheets.getSubimage (0,carTileSizey,carTileSize, carTileSizey);
                         break;
                     case CAR_3 :
                         obstacle = spriteSheets.getSubimage (0,2*carTileSizey,carTileSize, carTileSizey);
                         break;
                     case CAR_4 :
                         this.speed = -this.speed;
                         obstacle = spriteSheets.getSubimage (0,3*carTileSizey,carTileSize, carTileSizey);
                         break;
                     case CAR_5 :
                         this.speed = -this.speed;
                         obstacle = spriteSheets.getSubimage (0,4*carTileSizey,carTileSize, carTileSizey);
                         break;
                     case LOG_1 :
                         obstacle = spriteSheets2.getSubimage (0 ,0 +20,235, 45 +10);
                         break;
                     case LOG_2 :
                         obstacle = spriteSheets2.getSubimage (0,45 +20 +10 ,300, 45 +10);
                         break;
                     case LOG_3 :
                         obstacle = spriteSheets2.getSubimage (0,90 +20 +20 ,380, 45 +10);
                         break;
                     case LOG_4 :
                         obstacle = spriteSheets2.getSubimage (0,135 +20 +30 ,233, 45 +10);
                         break;
                     case LOG_5 :
                         obstacle = spriteSheets2.getSubimage (240,0 +20 ,135, 45 +10);
                         break;
                     case SNAKE :
                         anim = new BufferedImage[10]; // le serpent sera animé sur l'écran, on le représente par plusieurs images
                         for (int i = 0; i<5;i++) {
                             anim[i] = spriteSheets3.getSubimage(0, 33 * i, 100, 32 );
                             anim[9-i] = spriteSheets3.getSubimage(0, 33 * i, 100, 33 );
                         }

                         obstacle = anim[9];
                 }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void update () {
            x = x + speed ;
            if (speed > 0 && x > WIDTH) {  // un obstacle qui sort d'un coté rentre de l'autre coté
                x = -obstacle.getWidth();
            }
            else if (speed < 0 && x < -obstacle.getWidth()) {
                x = WIDTH;
            }
            if (type == TypeObstacle.SNAKE) {
                frameIndex ++ ;
                obstacle = anim[(int) (frameIndex/2) ];  // animation du snake
                if (frameIndex > 18) {
                    frameIndex = 0;
                }
            }

        }

        public void draw (Graphics2D g) {  // les obstacles ont des tailles différentes
            if (type == TypeObstacle.SNAKE){
                g.drawImage(obstacle, (int) x, (int) y, (int) (28 * 3), 27, null);
            }

            else if (type != TypeObstacle.LOG_1 && type != TypeObstacle.LOG_2  && type != TypeObstacle.LOG_3 && type != TypeObstacle.LOG_4 && type != TypeObstacle.LOG_5) {
                g.drawImage(obstacle, (int) x, (int) y, (int) (28 * 1.68), 28, null);
            }
            else if (type == TypeObstacle.LOG_1) {
                g.drawImage(obstacle, (int) x, (int) y, (int) (28 * 6), 27, null);
            }
            else if (type == TypeObstacle.LOG_2) {
                g.drawImage(obstacle, (int) x, (int) y, (int) (28 * 8), 27, null);
            }
            else if (type == TypeObstacle.LOG_3) {
                g.drawImage(obstacle, (int) x, (int) y, (int) (28 * 10), 27, null);
            }
            else if (type == TypeObstacle.LOG_4) {
                g.drawImage(obstacle, (int) x, (int) y, (int) (28 * 4), 27, null);
            }
            else if (type == TypeObstacle.LOG_5) {
                g.drawImage(obstacle, (int) x, (int) y, (int) (28 * 3), 27, null);
            }
        }
    }

    public class Lane extends BoxCollider {
        Obstacle[] obstacles ;
        float speed ;
        TypeObstacle type;
        float ww;

        public Lane (TypeObstacle type, int index, int n, float speed, float spacing) {
            super (0, index * 28, WIDTH, 28);
            obstacles = new Obstacle[n];
            this.speed = speed ;  // les éléments d'une voie ont tous le même type, la même vitese, ils sont identiques.
            this.type = type;


            if (type == TypeObstacle.SNAKE) {
                ww = (float) (2.5*27);
            }
            else if (type != TypeObstacle.LOG_1 && type != TypeObstacle.LOG_2  && type != TypeObstacle.LOG_3 && type != TypeObstacle.LOG_4 && type != TypeObstacle.LOG_5) {
                ww = (float) (1.5 * 27);
            }
            else if (type == TypeObstacle.LOG_1) {
                 ww = (float) (5.3*27);
            }
            else if (type == TypeObstacle.LOG_2) {
                 ww = (float) (7.5*27);
            }
            else if (type == TypeObstacle.LOG_3) {
                 ww = (float) (10*27);
            }
            else if (type == TypeObstacle.LOG_4) {
                ww = (float) (3*27);
            }
            else {
                 ww = (float) (2.5*27);
            }

            for (int i = 0; i < n; i++) {
                obstacles[i] = new Obstacle(type, spacing * i, y, ww, 27 , speed);
            }
        }

        public void check(Frog frog) {
            if (type != TypeObstacle.LOG_1 && type != TypeObstacle.LOG_2  && type != TypeObstacle.LOG_3 && type != TypeObstacle.LOG_4 && type != TypeObstacle.LOG_5) {
                for (Obstacle obstacle : obstacles) {
                    if (frog.intersects(obstacle)) {  // intersecter le snake ou la voiture fait perdre la grenouille
                        frog.resetFrog();
                    }
                }
            } else {
                boolean ok = false;
                for (Obstacle obstacle : obstacles) {
                    if (frog.intersects(obstacle)) {
                        ok = true;
                        frog.x += obstacle.speed;  // la grenouille est sur le rondin, elle se déplace avec celui-ci
                    }
                }
                if (!ok) {
                    frog.resetFrog();
                }
            }
        }

        public void update () {
            for (Obstacle obstacle : obstacles) {
                obstacle.update ();
            }
        }

        public void draw (Graphics2D g2) {
            for (Obstacle  obstacle : obstacles ) {
                obstacle.draw(g2);
            }
        }



    }




    public class Frog extends BoxCollider {
        BufferedImage frog;
        BufferedImage[] anim, frogAnimLeft, frogAnimRight, frogAnimUp, frogAnimDown;

        private int frameIndex;
        private boolean jumping;

        public Frog(float x, float y, float width, float height) {
            super(x,y, width, height);
            try {
                BufferedImage frogSpriteSheet = ImageIO.read(getClass().getResource("frog_ani2.png"));
                frogAnimDown = new BufferedImage[3];
                frogAnimUp  = new BufferedImage[3];  // différentes images de grenouilles pour différentes directions
                frogAnimRight = new BufferedImage[3];
                frogAnimLeft = new BufferedImage[3];
                anim = new BufferedImage[3];

                int frogTileSize = 79;
                int frogTileSizey = 60;
                for (int i = 0; i<3; i++) {

                    frogAnimDown[i] = frogSpriteSheet.getSubimage(  // prend un petit bout de l'image. On peut faire autrement au besoin
                            (2-i)*frogTileSize,
                            frogTileSizey * 2,
                            frogTileSize,
                            frogTileSizey
                    );

                    frogAnimUp[i] = frogSpriteSheet.getSubimage(
                            (2-i)*frogTileSize,
                            frogTileSizey * 3,
                            frogTileSize,
                            frogTileSizey
                    );

                    frogAnimRight[i] = frogSpriteSheet.getSubimage(
                            (2-i)*frogTileSize,
                            frogTileSizey,
                            frogTileSize,
                            frogTileSizey
                    );

                    frogAnimLeft[i] = frogSpriteSheet.getSubimage(
                            (2-i)*frogTileSize,
                            0,
                            frogTileSize,
                            frogTileSizey
                    );
                }

                frog = frogAnimUp[1];

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void resetFrog(){
              x = WIDTH/2 - width ;
              y = HEIGHT ;
            frog = frogAnimUp[0];  // la grenouille regarde vers l'avant en retombant au point de départ
        }

        public void update() {
            if (jumping) {
                frameIndex ++ ;
                frog = anim[(int) ((frameIndex/5.0) * (anim.length -1))];  // animation du mouvement de la grenouille
                if (frameIndex > 5) {
                    frameIndex = 0;
                    jumping = false;
                }
            }

            x = Math.min(Math.max(x,0), WIDTH - width);  // reste dans le cadre de jeu
            y = Math.min(Math.max(y,0), HEIGHT - height);
        }

        public void draw(Graphics2D g) {
            g.drawImage (frog, (int) x, (int) y -1, (int) width, (int) height, null);
        }

        public void jump (int xDir, int yDir) {  // déplacement de la grenouille
            frameIndex = 0;
            x += 28*xDir;
            y += 28* yDir;
        }

        public void keyPressed (KeyEvent e) {  // direction du déplacement
            if (e.getKeyCode () == KeyEvent.VK_UP) {
                jumping = true;
                anim = frogAnimUp;
                jump(0, -1);

            } else if (e.getKeyCode () == KeyEvent.VK_DOWN) {
                jumping = true;
                anim = frogAnimDown;
                jump(0, 1);

            } else if (e.getKeyCode () == KeyEvent.VK_RIGHT) {
                jumping = true;
                anim = frogAnimRight;
                jump(1, 0);

            } else if (e.getKeyCode () == KeyEvent.VK_LEFT) {
                jumping = true;
                anim = frogAnimLeft;
                jump(-1, 0);

            }
        }
    }


    public class Winner extends BoxCollider {
        boolean visible ;
        BufferedImage image;

        public Winner (float x , float y) {
            super (x,y,0,0);
            try {
                image = ImageIO.read(getClass().getResource("/win_4.png"));
                width = 28;
                height = 28;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void draw (Graphics2D g) {
            if (visible) {
                g.drawImage (image, (int) x, (int) y, (int) width, (int) height, null);
            }
        }
    }

    public class Loser extends BoxCollider {
        boolean falling ;
        BufferedImage image;
        int frameIndex;
        BufferedImage[] anim;

        public Loser (float x , float y) {
            super (x,y,0,0);
            try {
                BufferedImage frogSpriteSheet0 = ImageIO.read(getClass().getResource("/loose_1.jpg"));
                BufferedImage frogSpriteSheet1 = ImageIO.read(getClass().getResource("/loose_2.jfif"));
                BufferedImage frogSpriteSheet2 = ImageIO.read(getClass().getResource("/loose_3.png"));

                width = 28;
                height = 28;
                anim = new BufferedImage[3];
                anim[0] = frogSpriteSheet0;
                anim[1] = frogSpriteSheet1;
                anim[2] = frogSpriteSheet2;
                image = anim[2];
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void draw (Graphics2D g) {
                g.drawImage (image, (int) x, (int) y, (int) width, (int) height, null);
        }

        public void update () {
            if (falling) {
                frameIndex ++ ;
                image = anim[(int) ((frameIndex/10.0) * (anim.length -1))];
                if (frameIndex > 10) {
                    frameIndex = 0;
                    falling = false;
                }

            }
        }
    }


    /**
     * Classe mère des composants de la partie
     */
    public class BoxCollider {
        float x, y, width, height ;

        public BoxCollider (float x, float y, float width, float height) {
            this.width = width;
            this.height = height ;
            this.x = x;
            this.y = y;
        }

        boolean intersects (BoxCollider other) {
            float left = x;
            float right = x + width;
            float top = y;
            float bottom = y + height;

            float oLeft = other.x +5;
            float oRight = other.x + other.width;
            float oTop = other.y;
            float oBottom = other.y + other.height;

            return !(left >= oRight || right <= oLeft || top >= oBottom || bottom <= oTop);

        }


    }

}

