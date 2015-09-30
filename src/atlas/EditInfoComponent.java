package atlas;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.font.TextAttribute;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.List;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

@SuppressWarnings("serial")
public class EditInfoComponent extends JComponent {
	private Card[] cards;
	private JList<String> labelsList;
	private JComboBox<String> newLabelBox;
	private final DataManager dataManager;
	private JTextField nameField;
	//private JTextField imgPathField;
		
	public EditInfoComponent(DataManager dm, Card[] cards_) {
		dataManager = dm;
		cards = cards_;
		JLabel nameLabel = new JLabel("Name:");
		nameLabel.setFont(getLabelFont(nameLabel));
		nameField = makeNameField();
		//JLabel imgPathLabel = new JLabel("Image path: ");
		//imgPathLabel.setFont(getLabelFont(imgPathLabel));
		//imgPathField = makeImgPathField();
		JLabel labelsLabel = new JLabel("Labels:");
		JButton removelabelsButton = makeRemoveLabelsButton();
		labelsList = new JList<String>();
		labelsList.setLayoutOrientation(JList.HORIZONTAL_WRAP);
		labelsList.setVisibleRowCount(0);
		JScrollPane labelsListPane = new JScrollPane(labelsList);
		newLabelBox = new JComboBox<String>();
		newLabelBox.setEditable(true);
		refreshListContent();
		JButton addLabelButton = makeAddLabelButton();
		
		GroupLayout layout = new GroupLayout(this);
		setLayout(layout);
		layout.setAutoCreateGaps(true);
		layout.setAutoCreateContainerGaps(true);

		layout.setHorizontalGroup(
			layout.createParallelGroup()
				.addGroup(layout.createSequentialGroup()
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
						.addComponent(nameLabel)
						//.addComponent(imgPathLabel)
						.addComponent(labelsLabel))
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
						.addComponent(nameField)
						//.addComponent(imgPathField)
						.addComponent(removelabelsButton, GroupLayout.Alignment.TRAILING)))
				.addGroup(layout.createSequentialGroup()
					.addContainerGap(20,20)
					.addComponent(labelsListPane))
				.addGroup(layout.createSequentialGroup()
					.addContainerGap(20,20)
					.addComponent(newLabelBox)
					.addComponent(addLabelButton))
		);
		
		layout.setVerticalGroup(
			layout.createSequentialGroup()
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
					.addComponent(nameLabel)
					.addComponent(nameField)
				/*).addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
					.addComponent(imgPathLabel)
					.addComponent(imgPathField)*/
				).addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
					.addComponent(labelsLabel)
					.addComponent(removelabelsButton)
				).addComponent(labelsListPane)
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
					.addComponent(newLabelBox)
					.addComponent(addLabelButton)
				)
		);
	}
	
	public String getName() {
		return nameField.getText();
	}

	/*public String getImgPath() {
		return imgPathField.getText();
	}*/

	@SuppressWarnings({ "unchecked", "rawtypes" })
	//TODO bad practice?
	private Font getLabelFont(JLabel l) {
		if( cards.length == 1 ) return l.getFont();
		Font f = l.getFont();
		Map attrs = f.getAttributes();
		attrs.put(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);
		return new Font(attrs);
	}
	
	private JTextField makeNameField() {
		JTextField ret;
		if( cards.length == 1 ){
			ret = new JTextField(cards[0].name);
			ret.setEditable(true);
			return ret;
		}
		ret = new JTextField("--multiple cards selected--");
		ret.setEditable(false);
		return ret;
	}
	
	/*private JTextField makeImgPathField() {
		JTextField ret;
		if( cards.length == 1 ){
			ret = new JTextField(cards[0].imageFilename);
			ret.setEditable(true);
			return ret;
		}
		ret = new JTextField("Multiple cards selected");
		ret.setEditable(false);
		return ret;
	}*/
	
	private JButton makeRemoveLabelsButton() {
		JButton ret = new JButton("Remove selected labels");
		ret.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				List<String> selectedLabels = labelsList.getSelectedValuesList();
				for( Card c : cards )
					for( String s : selectedLabels )
						c.removeLabel(s);
				refreshListContent();
			}
		});
		return ret;
	}
	
	private JButton makeAddLabelButton() {
		JButton ret = new JButton("Add selected label");
		ret.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String selection = (String)newLabelBox.getSelectedItem();
				for( Card c : cards )
					c.addLabel(selection);
				refreshListContent();
			}
		});
		return ret;
	}
	
	/**
	 * the contents of 'labels', 'labelsList', and 'newLabelBox' are updated to reflect
	 * the current state of the selected cards
	 */
	private void refreshListContent() {
		//remove any label not associated with the given card, so the list reflects
		//  only labels associated with all cards 
		ArrayList<String> labels = Card.removeStringsFromList(
				new ArrayList<String>(cards[0].labels), 
				Card.getAutoLabels());
		ArrayList<String> toRemove = new ArrayList<String>();
		for( Card c : cards ){
			toRemove.clear();
			for( String s : labels ){
				if( !c.labels.contains(s) )
					toRemove.add(s);
			}
			labels.removeAll(toRemove);
			if( labels.isEmpty() ) break;
		}
		
		//create prototype cell
		String longest = "";
		for( String s : labels ) if( s.length() > longest.length() ) longest = s;
		labelsList.setPrototypeCellValue(longest + "WWW"); //add some buffer room for readability
		
		labelsList.setListData(labels.toArray(new String[0]));
		ArrayList<String> remainingLabels = Card.removeStringsFromList(
				new ArrayList<String>(Arrays.asList(dataManager.getLabels())), 
				Card.getAutoLabels());
		remainingLabels.remove(Card.ALL);
		remainingLabels.removeAll(labels);
		newLabelBox.removeAllItems();
		Collections.sort(remainingLabels);
		for( String label : remainingLabels ){
			newLabelBox.addItem(label);
		}
	}
	
}
