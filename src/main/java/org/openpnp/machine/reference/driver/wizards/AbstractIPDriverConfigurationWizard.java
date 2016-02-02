package org.openpnp.machine.reference.driver.wizards;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.machine.reference.driver.AbstractIPDriver;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

public class AbstractIPDriverConfigurationWizard extends AbstractConfigurationWizard {
    private final AbstractIPDriver driver;
    private JTextField textFieldHost;
    private JTextField textFieldPort;
    
    public AbstractIPDriverConfigurationWizard(AbstractIPDriver driver) {
        this.driver = driver;
        
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        
        JPanel panel = new JPanel();
        contentPanel.add(panel);
        panel.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));
        
        JLabel lblPortName = new JLabel("Host");
        panel.add(lblPortName, "2, 2, right, default");
        
        textFieldHost = new JTextField("localhost", 20);
        panel.add(textFieldHost, "4, 2, fill, default");
        
        JLabel lblBaudRate = new JLabel("Port");
        panel.add(lblBaudRate, "2, 4, right, default");
        
        textFieldPort = new JTextField("4242", 20);
        panel.add(textFieldPort, "4, 4, fill, default");
    }

    
    @Override
    public void createBindings() {
        IntegerConverter integerConverter = new IntegerConverter();
        //TODO: this crashes
        //addWrappedBinding(driver, "host", textFieldHost, "text");
        //addWrappedBinding(driver, "port", textFieldPort, "text");
    }
}
