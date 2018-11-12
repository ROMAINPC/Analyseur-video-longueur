/*******************************************************************************
 * Copyright (C) 2017 ROMAINPC_LECHAT
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package application;
	
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;

import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.Pair;


public class AnalyseVideoLongueur extends Application {
	
	private File fichier;
	private Media video = null;
	private double fps = 30;
	
	public void start(Stage primaryStage) {
		try {
			Group root = new Group();
			Scene scene = new Scene(root, 400, 400);
			primaryStage.setTitle("Analyse vid�o de longueur, par RPC");
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			primaryStage.setScene(scene);
			primaryStage.show();
			
			//premi�re info:
			Alert infoChoixVideo = new Alert(Alert.AlertType.INFORMATION);
			infoChoixVideo.setTitle("Instructions"); infoChoixVideo.setContentText("Vous devez choisir quel fichier vid�o doit �tre analys�.\nCe logiciel a �t� r�alis� par Romain Pigret-Cadou");
			infoChoixVideo.setHeaderText("");
			infoChoixVideo.initOwner(primaryStage);
			infoChoixVideo.show();
			
			FileChooser fC = new FileChooser();
			
			
			infoChoixVideo.setOnHidden(event -> {
				//choix du fichier:
				fichier = fC.showOpenDialog(primaryStage);
				
				
				//choix du pt de d�part:
				Dialog<Pair<String,String>> departAnalyse = new Dialog<Pair<String,String>>();
				departAnalyse.setHeaderText(
						"Veuillez s�lectionner le point de d�part (en secondes) de votre vid�o pour l'analyse\n"
						+ "(il sera possible de passer les premi�res images).");
				departAnalyse.setTitle("Instructions");
				
				departAnalyse.setHeight(200);
				ComboBox<Double> cB = new ComboBox<Double>();
				try {
				video = new Media(fichier.toURI().toString());
				MediaPlayer mP = new MediaPlayer(video);
				mP.setOnReady(new Runnable(){
					public void run() {
						for(double i = 0 ; i <= video.getDuration().toSeconds() ; i++)
							cB.getItems().add(i);
						cB.setValue(0d);
						departAnalyse.getDialogPane().setContent(cB);
						departAnalyse.setOnHidden(event2 ->{
						departAnalyse.hide();
							
							//---->choix des fps
							Dialog<Pair<String,String>> selFPS = new Dialog<Pair<String,String>>();
							selFPS.setHeaderText(
								"Veuillez s�lectionner le nombre d'images par secondes de votre vid�o\n"
								+ "Clique droit > Propri�t�s > D�tails > Fr�quence d'images");
							selFPS.setTitle("Instructions");
							selFPS.setHeight(200);
							ComboBox<Double> choix = new ComboBox<Double>();
							for(double i = 1 ; i <= 1000 ; i++)
								choix.getItems().add(i);
							choix.setValue(30d);
							selFPS.getDialogPane().setContent(choix);
							selFPS.setOnHidden(event3 ->{
								selFPS.hide();
								fps = choix.getValue();
								//d�part de l'analyse:
								departAnalyse(cB.getValue(), mP, primaryStage, fps);
							});
							selFPS.getDialogPane().getButtonTypes().add(new ButtonType("Valider", ButtonData.APPLY));
							selFPS.show();
							
							
						});
						departAnalyse.getDialogPane().getButtonTypes().add(new ButtonType("Valider", ButtonData.APPLY));
						departAnalyse.show();
					}
				});
				} catch(Exception e) {
					//en cas de fichier invalide:
					Alert erreurFormat = new Alert(Alert.AlertType.ERROR);
					erreurFormat.setTitle("Erreur de format"); erreurFormat.setContentText("Le format vid�o n'est pas pris en charge");
					erreurFormat.show();
					video = null;
					primaryStage.hide();
				}
				
				
			});
			
			
			
			
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	//variables et objets:
	private boolean isCommence = false;
	private Label infos = new Label("D�finition de l'�chelle : Cliquez sur la vid�o");
	private double eX1 = 0; private double eY1 = 0; private double eX2 = 0; private double eY2 = 0;
	private int count = 0;
	private int countImg = 1;
	private Circle pt1 = new Circle(4); private Circle pt2 = new Circle(4);
	private Line ligne = new Line();
	private double echelleVideo = 0; private double echelleReelle = 0;
	private boolean echelle =  true;
	private boolean estUnNombre = false;
	private double premierTps = 0;
	
	//stockage des r�sulatats:
	private ArrayList<Double> resultats = new ArrayList<Double>();
	
	//Mise en place de l'analyse:
	private void departAnalyse(double depart, MediaPlayer lecteur, Stage fenetre, double nbFPS) {
		
		fenetre.setMaximized(true);
		fenetre.show();
		MediaView mV = new MediaView(lecteur);
		mV.setFitHeight(fenetre.getScene().getHeight() - 50);
		GridPane groupe = new GridPane();
		Group grPts = (Group) fenetre.getScene().getRoot();
		grPts.getChildren().add(groupe);
		
		//d�finitions des points d'aide:
		pt1.setFill(Color.rgb(0, 255, 0)); pt2.setFill(Color.rgb(0, 255, 0));
		pt1.setLayoutX(-10); pt1.setLayoutY(-10);
		pt2.setLayoutX(-10); pt2.setLayoutY(-10);
		ligne.setFill(Color.RED); ligne.setStrokeWidth(5); ligne.setStroke(Color.RED);
		grPts.getChildren().addAll(pt1, pt2, ligne);
		
		//affichages des composants:
		Button passer = new Button("Passer une image");
		passer.setMinHeight(50); passer.setFont(Font.font("Comic Sans MS", FontWeight.BOLD, 15));
		passer.setTextFill(Color.RED); passer.setMinWidth(fenetre.getScene().getWidth() / 2);
		Button debutStop = new Button("Commencer l'analyse");
		debutStop.setMinHeight(50); debutStop.setFont(Font.font("Comic Sans MS", FontWeight.BOLD, 15));
		debutStop.setTextFill(Color.RED); debutStop.setMinWidth(fenetre.getScene().getWidth() / 2);
		groupe.add(mV, 1, 1, 2, 1);
		groupe.add(passer, 1, 2);
		groupe.add(debutStop, 2, 2);
		
		
		
		
		//depart et phase de passage des images:
		mV.getMediaPlayer().setStartTime(Duration.seconds(depart));
		
		//actions boutons:
		passer.setOnAction(event -> {
			mV.getMediaPlayer().setStartTime(mV.getMediaPlayer().getStartTime().add(Duration.millis(1000 / nbFPS)));
		});
		
		debutStop.setOnAction(event ->{
			if(!isCommence){
				//d�part de l'analyse:
				passer.setDisable(true);
				debutStop.setDisable(true);
				groupe.getChildren().remove(passer);
				infos.setFont(Font.font("Comic Sans MS", FontWeight.BOLD, 15));
				infos.setPrefWidth(fenetre.getScene().getWidth() / 2);
				groupe.add(infos, 1, 2);
				isCommence = true;
				debutStop.setText("R�cup�rer les r�sultats");
				
				
				//s�lection de l'�chelle:
				Alert infoEchelle = new Alert(Alert.AlertType.INFORMATION);
				infoEchelle.setTitle("Instructions"); infoEchelle.setContentText("Cliquez successivement sur deux points s�par�s dans l'espace\npour d�finir l'�chelle pour votre analyse");
				infoEchelle.setHeaderText("");
				infoEchelle.initOwner(fenetre);
				infoEchelle.show();
				mV.setOnMouseClicked(event2 -> {
					if(echelle){
						if(count == 0){
							eX1 = event2.getX();
							eY1 = event2.getY();
							count++;
							infos.setText("D�finition de l'�chelle : Premier point plac�, cliquez � nouveau");
							pt1.setLayoutX(eX1); pt1.setLayoutY(eY1);
							premierTps = mV.getMediaPlayer().getStartTime().toSeconds();
						} else {
							count++;
							eX2 = event2.getX();
							eY2 = event2.getY();
							infos.setText("D�finition de l'�chelle : Points plac�s");
							pt2.setLayoutX(eX2); pt2.setLayoutY(eY2);
							echelleVideo = Math.sqrt( (eX2 - eX1)*(eX2 - eX1) + (eY2 - eY1)*(eY2 - eY1) );
							ligne.setStartX(eX1); ligne.setStartY(eY1); ligne.setEndX(eX2); ligne.setEndY(eY2);
							
							Dialog<Pair<String,String>> defEchelle = new Dialog<Pair<String,String>>();
							defEchelle.setTitle("Instructions"); defEchelle.setHeaderText("Entrez la longueur r�elle(en vrai) de la distance s�lectionn�e:\n/!\\En m�tres, et mettez un point au lieu d'une virgule\nSi vous fermez la fen�tre(croix) la longueur sera �gale � la longueur mesur�e(en pixels)");
							defEchelle.getDialogPane().getButtonTypes().add(new ButtonType("Valider", ButtonData.APPLY));
							TextField champs = new TextField();
							champs.setPromptText("nombre uniquement (virgule possible)");
							defEchelle.getDialogPane().setContent(champs);
							defEchelle.show();
							
							Alert infoAnalyse = new Alert(Alert.AlertType.INFORMATION);
							infoAnalyse.setTitle("Instructions");
							infoAnalyse.setContentText("Maintenant pour chaque image de la vid�o,\n"
									+ "placez 2 points aux extr�mit�s de la longueur a mesurer.\n"
									+ "Le logiciel vous fera passer automatiquement les images.\n");
							infoAnalyse.setHeaderText("");
							infoAnalyse.initOwner(fenetre);
							
							defEchelle.setOnCloseRequest(event3 -> {
								echelleReelle = echelleVideo;
								infos.setText("Image n�" + countImg + " : Placez le premier point");
								count = 0;
								grPts.getChildren().removeAll(ligne, pt1, pt2);
								infoAnalyse.show();
							});
							defEchelle.setOnHidden(event3 ->{
								try {
									Double.valueOf(champs.getText());
									estUnNombre = true;
								}
								catch(Exception e){
									estUnNombre = false;
								}
								
								if(estUnNombre){
									echelleReelle = Double.valueOf(champs.getText());
								} else {
									defEchelle.show();
								}
							});
							
							echelle = false;
						}
						
					} else {
						//analyse image par image:
						if(count == 0){
							count++;
							debutStop.setDisable(true);
							infos.setText("Image n�" + countImg + " : Placez le deuxi�me point");
							eX1 = event2.getX();
							eY1 = event2.getY();
							
							
						}else{
							count = 0;
							debutStop.setDisable(false);
							mV.getMediaPlayer().setStartTime(mV.getMediaPlayer().getStartTime().add(Duration.millis(1000 / nbFPS)));
							countImg++;
							infos.setText("Image n�" + countImg + " : Placez le premier point");
							eX2 = event2.getX();
							eY2 = event2.getY();
							
							resultats.add(Math.sqrt( (eX2 - eX1)*(eX2 - eX1) + (eY2 - eY1)*(eY2 - eY1) ) * echelleReelle / echelleVideo);
							
						}
						
					}
				});
				
				
				
			}else{
				//arret de l'analyse et sauvegarde:
				debutStop.setDisable(true);
				
				ArrayList<String> r = new ArrayList<String>();
				
				for(int i = 0 ; i < resultats.size() ; i++){
					String str = resultats.get(i).toString();
					String str2 = "";
					for(int j = 0 ; j < str.length() ; j++){
						if(str.charAt(j) == '.')
							str2+=',';
						else
							str2+=str.charAt(j);
					}
					r.add(str2);
				}
				
				ArrayList<String> r2 = new ArrayList<String>();
				ArrayList<String> r3 = new ArrayList<String>();
				double val = 0;
				double dep = premierTps;
				for(int i = 0 ; i < countImg - 1 ; i++){
					String str = String.valueOf(val);
					String str2 = "";
					for(int j = 0 ; j < str.length() ; j++){
						if(str.charAt(j) == '.')
							str2+=',';
						else
							str2+=str.charAt(j);
					}
					r2.add(str2);
					val += 1 / fps;
					
					String str3 = String.valueOf(dep);
					String str4 = "";
					for(int j = 0 ; j < str3.length() ; j++){
						if(str3.charAt(j) == '.')
							str4+=',';
						else
							str4+=str3.charAt(j);
					}
					r3.add(str4);
					dep += 1 / fps;
					
				}
				
				//sauvegarde
				File file;
				FileWriter fw ;
				try {
					file = new File("resultats.txt");
					String retour = System.getProperty("line.separator");
					fw = new FileWriter(file);
					
					fw.write("LISTES DES LONGUEURS EN FONCTION DU TEMPS");
					fw.write(retour);
					for(int i = 0 ; i < r.size() ; i++){
						fw.write(r.get(i));
						fw.write(retour);
					}
					fw.write(retour);
					
					fw.write("LISTES DES TEMPS A PARTIR DE ZERO");
					fw.write(retour);
					for(int i = 0 ; i < r2.size() ; i++){
						fw.write(r2.get(i));
						fw.write(retour);
					}
					fw.write(retour);
					
					fw.write("LISTES DES TEMPS A PARTIR DE LA PREMIERE LONGUEUR");
					fw.write(retour);
					for(int i = 0 ; i < r3.size() ; i++){
						fw.write(r3.get(i));
						fw.write(retour);
					}
					fw.write(retour);
					
					fw.close();
				} catch(Exception e) {
					e.printStackTrace();
				}
				
				//message et fin.
				Alert fin = new Alert(Alert.AlertType.INFORMATION);
				fin.setTitle("Termin�");
				fin.setContentText("L'analyse est termin�e, vous retrouverez vos r�sultats dans le fichier .txt � c�t� de l'ex�cutable de ce logiciel.\n"
						+ "Vous disposerez de 3 listes de donn�es:\n"
						+ "   -> La longueur que vous avez mesur�e en m�tres\n"
						+ "   -> L'instant en secondes de chaque longueur mesur�e � partir de 0\n"
						+ "   -> L'instant en secondes de chaque longueur mesur�e � partir de la premi�re longueur mesur�e\n\n Bonne continuation :)");
				fin.setHeaderText("");
				fin.initOwner(fenetre);
				fin.show();
				fin.setOnHidden(event2 -> fenetre.hide());
				
				
			}
		});
		
	}

	public static void main(String[] args) {
		launch(args);
	}
}
