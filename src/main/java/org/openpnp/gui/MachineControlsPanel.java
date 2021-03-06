/*
 	Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
 	
 	This file is part of OpenPnP.
 	
	OpenPnP is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenPnP is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenPnP.  If not, see <http://www.gnu.org/licenses/>.
 	
 	For more information about OpenPnP visit http://openpnp.org
 */

package org.openpnp.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.FocusTraversalPolicy;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Hashtable;
import java.util.Locale;
import java.util.concurrent.Callable;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.openpnp.ConfigurationListener;
import org.openpnp.gui.components.CameraPanel;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.gui.support.NozzleItem;
import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Head;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.JobProcessor;
import org.openpnp.spi.Machine;
import org.openpnp.spi.MachineListener;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.PasteDispenser;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.UiUtils;

import com.google.common.util.concurrent.FutureCallback;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

public class MachineControlsPanel extends JPanel {
	private final JFrame frame;
	private final CameraPanel cameraPanel;
	private final Configuration configuration;

	private Nozzle selectedNozzle;

	private JTextField textFieldX;
	private JTextField textFieldY;
	private JTextField textFieldC;
	private JTextField textFieldZ;
	private JButton btnStartStop;
	private JSlider sliderIncrements;
    private JComboBox comboBoxNozzles;
	
	
	private Color startColor = Color.green;
	private Color stopColor = new Color(178, 34, 34);
	private Color droNormalColor = new Color(0xBDFFBE);
	private Color droEditingColor = new Color(0xF0F0A1);
	private Color droWarningColor = new Color(0xFF5C5C);
	private Color droSavedColor = new Color(0x90cce0);
	
	private JogControlsPanel jogControlsPanel;
	private JDialog jogControlsWindow;
	
	private volatile double savedX = Double.NaN, savedY = Double.NaN, savedZ = Double.NaN, savedC = Double.NaN; 
	
	/**
	 * Create the panel.
	 */
	public MachineControlsPanel(Configuration configuration, JFrame frame, CameraPanel cameraPanel) {
		this.frame = frame;
		this.cameraPanel = cameraPanel;
		this.configuration = configuration;
		
		jogControlsPanel = new JogControlsPanel(configuration, this, frame);
		
		createUi();
		
		configuration.addListener(configurationListener);

		jogControlsWindow = new JDialog(frame, "Jog Controls");
		jogControlsWindow.setResizable(false);
		jogControlsWindow.getContentPane().setLayout(new BorderLayout());
		jogControlsWindow.getContentPane().add(jogControlsPanel);
	}
	
	public void setSelectedNozzle(Nozzle nozzle) {
	    selectedNozzle = nozzle;
	    comboBoxNozzles.setSelectedItem(selectedNozzle);
	    updateDros();
	}
	
    public Nozzle getSelectedNozzle() {
        return selectedNozzle;
    }
    
    public PasteDispenser getSelectedPasteDispenser() {
        try {
            // TODO: We don't actually have a way to select a dispenser yet, so
            // until we do we just return the first one.
            return Configuration
            		.get()
            		.getMachine()
            		.getDefaultHead()
            		.getDefaultPasteDispenser();
        }
        catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Returns the selected Nozzle or PasteDispenser depending on which type
     * of Job is selected.
     * @return
     */
    public HeadMountable getSelectedTool() {
    	JobProcessor.Type jobProcessorType = MainFrame.jobPanel.getSelectedJobProcessorType(); 
        if (jobProcessorType == JobProcessor.Type.PickAndPlace) {
            return getSelectedNozzle();
        }
        else if (jobProcessorType == JobProcessor.Type.SolderPaste) {
            return getSelectedPasteDispenser();
        }
        else {
            throw new Error("Unknown tool type: " + jobProcessorType);
        }
    }
    
	public JogControlsPanel getJogControlsPanel() {
		return jogControlsPanel;
	}
	
	private void setUnits(LengthUnit units) {
		if (units == LengthUnit.Millimeters) {
			Hashtable<Integer, JLabel> incrementsLabels = new Hashtable<Integer, JLabel>();
			incrementsLabels.put(1, new JLabel("0.01"));
			incrementsLabels.put(2, new JLabel("0.1"));
			incrementsLabels.put(3, new JLabel("1.0"));
            incrementsLabels.put(4, new JLabel("10"));
            incrementsLabels.put(5, new JLabel("100"));
			sliderIncrements.setLabelTable(incrementsLabels);
		}
		else if (units == LengthUnit.Inches) {
			Hashtable<Integer, JLabel> incrementsLabels = new Hashtable<Integer, JLabel>();
			incrementsLabels.put(1, new JLabel("0.001"));
			incrementsLabels.put(2, new JLabel("0.01"));
			incrementsLabels.put(3, new JLabel("0.1"));
            incrementsLabels.put(4, new JLabel("1.0"));
            incrementsLabels.put(5, new JLabel("10.0"));
			sliderIncrements.setLabelTable(incrementsLabels);
		}
		else {
			throw new Error("setUnits() not implemented for " + units);
		}
		updateDros();
	}

	public double getJogIncrement() {
		if (configuration.getSystemUnits() == LengthUnit.Millimeters) {
			return 0.01 * Math.pow(10, sliderIncrements.getValue() - 1);
		}
		else if (configuration.getSystemUnits() == LengthUnit.Inches) {
			return 0.001 * Math.pow(10, sliderIncrements.getValue() - 1);
		}
		else {
			throw new Error("getJogIncrement() not implemented for " + configuration.getSystemUnits());
		}
	}
	
	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		homeAction.setEnabled(enabled);
		goToZeroAction.setEnabled(enabled);
		jogControlsPanel.setEnabled(enabled);
		targetCameraAction.setEnabled(enabled);
		targetToolAction.setEnabled(enabled);
	}
	
	public Location getCurrentLocation() {
        if (selectedNozzle == null) {
            return null;
        }
        
        Location l = selectedNozzle.getLocation();
        l = l.convertToUnits(configuration.getSystemUnits());
        
        return l;
	}
	
	public void updateDros() {
	    Location l = getCurrentLocation();
	    if (l == null) {
	        return;
	    }
	    
		double x, y, z, c;
		
		x = l.getX();
		y = l.getY();
		z = l.getZ();
		c = l.getRotation();
		
        double savedX = this.savedX;
		if (!Double.isNaN(savedX)) {
            x -= savedX;
        }
        
        double savedY = this.savedY;
        if (!Double.isNaN(savedY)) {
            y -= savedY;
        }
        
        double savedZ = this.savedZ;
        if (!Double.isNaN(savedZ)) {
            z -= savedZ;
        }
        
        double savedC = this.savedC;
        if (!Double.isNaN(savedC)) {
            c -= savedC;
        }
        
		textFieldX.setText(String.format(Locale.US,configuration.getLengthDisplayFormat(), x));
		textFieldY.setText(String.format(Locale.US,configuration.getLengthDisplayFormat(), y));
		textFieldZ.setText(String.format(Locale.US,configuration.getLengthDisplayFormat(), z));
		textFieldC.setText(String.format(Locale.US,configuration.getLengthDisplayFormat(), c));
	}
	
	private void createUi() {
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		ButtonGroup buttonGroup = new ButtonGroup();
		
		JPanel panel = new JPanel();
		add(panel);
		panel.setLayout(new FormLayout(new ColumnSpec[] {
		        FormSpecs.RELATED_GAP_COLSPEC,
		        ColumnSpec.decode("default:grow"),},
		    new RowSpec[] {
		        FormSpecs.RELATED_GAP_ROWSPEC,
		        FormSpecs.DEFAULT_ROWSPEC,}));
		
		comboBoxNozzles = new JComboBox();
		comboBoxNozzles.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setSelectedNozzle(((NozzleItem) comboBoxNozzles.getSelectedItem()).getNozzle());
            }
        });
		panel.add(comboBoxNozzles, "2, 2, fill, default");
		
		JPanel panelDrosParent = new JPanel();
		add(panelDrosParent);
		panelDrosParent.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		
		JPanel panelDros = new JPanel();
		panelDrosParent.add(panelDros);
		panelDros.setLayout(new BoxLayout(panelDros, BoxLayout.Y_AXIS));
		
		JPanel panelDrosFirstLine = new JPanel();
		panelDros.add(panelDrosFirstLine);
		panelDrosFirstLine.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		
		JLabel lblX = new JLabel("X");
		lblX.setFont(new Font("Lucida Grande", Font.BOLD, 24));
		panelDrosFirstLine.add(lblX);
		
		textFieldX = new JTextField();
		textFieldX.setEditable(false);
		textFieldX.setFocusTraversalKeysEnabled(false);
		textFieldX.setSelectionColor(droEditingColor);
		textFieldX.setDisabledTextColor(Color.BLACK);
		textFieldX.setBackground(droNormalColor);
		textFieldX.setFont(new Font("Lucida Grande", Font.BOLD, 24));
		textFieldX.setText("0000.0000");
		textFieldX.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    savedX = Double.NaN;
                }
                saveXAction.actionPerformed(null);
            }
        });
		panelDrosFirstLine.add(textFieldX);
		textFieldX.setColumns(6);
		
		Component horizontalStrut = Box.createHorizontalStrut(15);
		panelDrosFirstLine.add(horizontalStrut);
		
		JLabel lblY = new JLabel("Y");
		lblY.setFont(new Font("Lucida Grande", Font.BOLD, 24));
		panelDrosFirstLine.add(lblY);
		
		textFieldY = new JTextField();
		textFieldY.setEditable(false);
		textFieldY.setFocusTraversalKeysEnabled(false);
		textFieldY.setSelectionColor(droEditingColor);
		textFieldY.setDisabledTextColor(Color.BLACK);
		textFieldY.setBackground(droNormalColor);
		textFieldY.setFont(new Font("Lucida Grande", Font.BOLD, 24));
		textFieldY.setText("0000.0000");
        textFieldY.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    savedY = Double.NaN;
                }
                saveYAction.actionPerformed(null);
            }
        });
		panelDrosFirstLine.add(textFieldY);
		textFieldY.setColumns(6);
		
		JButton btnTargetTool = new JButton(targetToolAction);
		panelDrosFirstLine.add(btnTargetTool);
		btnTargetTool.setToolTipText("Position the tool at the camera's current location.");
		
		JPanel panelDrosSecondLine = new JPanel();
		panelDros.add(panelDrosSecondLine);
		panelDrosSecondLine.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		
		JLabel lblC = new JLabel("C");
		lblC.setFont(new Font("Lucida Grande", Font.BOLD, 24));
		panelDrosSecondLine.add(lblC);
		
		textFieldC = new JTextField();
		textFieldC.setEditable(false);
		textFieldC.setFocusTraversalKeysEnabled(false);
		textFieldC.setSelectionColor(droEditingColor);
		textFieldC.setDisabledTextColor(Color.BLACK);
		textFieldC.setBackground(droNormalColor);
		textFieldC.setText("0000.0000");
		textFieldC.setFont(new Font("Lucida Grande", Font.BOLD, 24));
		textFieldC.setColumns(6);
        textFieldC.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    savedC = Double.NaN;
                }
                saveCAction.actionPerformed(null);
            }
        });
		panelDrosSecondLine.add(textFieldC);
		
		Component horizontalStrut_1 = Box.createHorizontalStrut(15);
		panelDrosSecondLine.add(horizontalStrut_1);
		
		JLabel lblZ = new JLabel("Z");
		lblZ.setFont(new Font("Lucida Grande", Font.BOLD, 24));
		panelDrosSecondLine.add(lblZ);
		
		textFieldZ = new JTextField();
		textFieldZ.setEditable(false);
		textFieldZ.setFocusTraversalKeysEnabled(false);
		textFieldZ.setSelectionColor(droEditingColor);
		textFieldZ.setDisabledTextColor(Color.BLACK);
		textFieldZ.setBackground(droNormalColor);
		textFieldZ.setText("0000.0000");
		textFieldZ.setFont(new Font("Lucida Grande", Font.BOLD, 24));
		textFieldZ.setColumns(6);
        textFieldZ.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    savedZ = Double.NaN;
                }
                saveZAction.actionPerformed(null);
            }
        });
		panelDrosSecondLine.add(textFieldZ);
		
		JButton btnTargetCamera = new JButton(targetCameraAction);
		panelDrosSecondLine.add(btnTargetCamera);
		btnTargetCamera.setToolTipText("Position the camera at the tool's current location.");
		
		JPanel panelIncrements = new JPanel();
		add(panelIncrements);
		
		sliderIncrements = new JSlider();
		panelIncrements.add(sliderIncrements);
		sliderIncrements.setMajorTickSpacing(1);
		sliderIncrements.setValue(1);
		sliderIncrements.setSnapToTicks(true);
		sliderIncrements.setPaintLabels(true);
		sliderIncrements.setPaintTicks(true);
		sliderIncrements.setMinimum(1);
		sliderIncrements.setMaximum(5);
		
		JPanel panelStartStop = new JPanel();
		add(panelStartStop);
		panelStartStop.setLayout(new BorderLayout(0, 0));
		
		btnStartStop = new JButton(startMachineAction);
		btnStartStop.setFocusable(true);
		btnStartStop.setForeground(startColor);
		panelStartStop.add(btnStartStop);
		btnStartStop.setFont(new Font("Lucida Grande", Font.BOLD, 48));
		btnStartStop.setPreferredSize(new Dimension(160, 70));
		
		setFocusTraversalPolicy(focusPolicy);
		setFocusTraversalPolicyProvider(true);
	}
	
	private FocusTraversalPolicy focusPolicy = new FocusTraversalPolicy() {
		@Override
		public Component getComponentAfter(Container aContainer,
				Component aComponent) {
			return sliderIncrements;
		}

		@Override
		public Component getComponentBefore(Container aContainer,
				Component aComponent) {
			return sliderIncrements;
		}

		@Override
		public Component getDefaultComponent(Container aContainer) {
			return sliderIncrements;
		}

		@Override
		public Component getFirstComponent(Container aContainer) {
			return sliderIncrements;
		}

		@Override
		public Component getInitialComponent(Window window) {
			return sliderIncrements;
		}

		@Override
		public Component getLastComponent(Container aContainer) {
			return sliderIncrements;
		}
	};
	
	@SuppressWarnings("serial")
	private Action stopMachineAction = new AbstractAction("STOP") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			setEnabled(false);
			Configuration.get().getMachine().submit(new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					Configuration.get().getMachine().setEnabled(false);
					return null;
				}
			}, new FutureCallback<Void>() {
				@Override
				public void onSuccess(Void result) {
					setEnabled(true);
				}

				@Override
				public void onFailure(Throwable t) {
					MessageBoxes.errorBox(MachineControlsPanel.this, "Stop Failed", t.getMessage());
					setEnabled(true);
				}
			}, true);
		}
	};
	
	@SuppressWarnings("serial")
	private Action startMachineAction = new AbstractAction("START") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			setEnabled(false);
			Configuration.get().getMachine().submit(new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					Configuration.get().getMachine().setEnabled(true);
					return null;
				}
			}, new FutureCallback<Void>() {
				@Override
				public void onSuccess(Void result) {
					setEnabled(true);
				}

				@Override
				public void onFailure(Throwable t) {
					MessageBoxes.errorBox(MachineControlsPanel.this, "Start Failed", t.getMessage());
					setEnabled(true);
				}
			}, true);
		}
	};
	
	@SuppressWarnings("serial")
	public Action goToZeroAction = new AbstractAction("Go To Zero") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			UiUtils.submitUiMachineTask(() -> {
				selectedNozzle.moveToSafeZ(1.0);
				// Move to 0, 0, 0, 0.
				selectedNozzle.moveTo(new Location(LengthUnit.Millimeters, 0, 0, 0, 0), 1.0);
			});
		}
	};
	
	@SuppressWarnings("serial")
	public Action homeAction = new AbstractAction("Home") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			UiUtils.submitUiMachineTask(() -> {
				selectedNozzle.getHead().home();
			});
		}
	};
	
	public Action showHideJogControlsWindowAction = new AbstractAction("Show Jog Controls") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			if (jogControlsWindow.isVisible()) {
			    // Hide
				jogControlsWindow.setVisible(false);
				putValue(AbstractAction.NAME, "Show Jog Controls");
			}
			else {
			    // Show
				jogControlsWindow.setVisible(true);
				jogControlsWindow.pack();
				int x = (int) getLocationOnScreen().getX();
				int y = (int) getLocationOnScreen().getY();
				x += (getSize().getWidth() / 2) - (jogControlsWindow.getSize().getWidth() / 2);
				y += getSize().getHeight();
				jogControlsWindow.setLocation(x, y);
                putValue(AbstractAction.NAME, "Hide Jog Controls");
			}
		}
	};
	
	@SuppressWarnings("serial")
	public Action raiseIncrementAction = new AbstractAction("Raise Jog Increment") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			sliderIncrements.setValue(Math.min(sliderIncrements.getMaximum(), sliderIncrements.getValue() + 1));
		}
	};
	
	@SuppressWarnings("serial")
	public Action lowerIncrementAction = new AbstractAction("Lower Jog Increment") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			sliderIncrements.setValue(Math.max(sliderIncrements.getMinimum(), sliderIncrements.getValue() - 1));
		}
	};
	
	@SuppressWarnings("serial")
	public Action targetToolAction = new AbstractAction(null, Icons.centerTool) {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			UiUtils.submitUiMachineTask(() -> {
				HeadMountable tool = getSelectedTool();
				Camera camera = tool.getHead().getDefaultCamera();
				MovableUtils.moveToLocationAtSafeZ(tool, camera.getLocation(), 1.0);
			});
		}
	};
	
    @SuppressWarnings("serial")
    public Action targetCameraAction = new AbstractAction(null, Icons.centerCamera) {
        @Override
        public void actionPerformed(ActionEvent arg0) {
			UiUtils.submitUiMachineTask(() -> {
				HeadMountable tool = getSelectedTool();
				Camera camera = tool.getHead().getDefaultCamera();
				MovableUtils.moveToLocationAtSafeZ(camera, tool.getLocation(), 1.0);
			});
        }
    };
    
    @SuppressWarnings("serial")
    public Action saveXAction = new AbstractAction(null) {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            if (Double.isNaN(savedX)) {
                textFieldX.setBackground(droSavedColor);
                savedX = getCurrentLocation().getX();
            }
            else {
                textFieldX.setBackground(droNormalColor);
                savedX = Double.NaN;
            }
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    updateDros();
                }
            });
        }
    };
    
    @SuppressWarnings("serial")
    public Action saveYAction = new AbstractAction(null) {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            if (Double.isNaN(savedY)) {
                textFieldY.setBackground(droSavedColor);
                savedY = getCurrentLocation().getY();
            }
            else {
                textFieldY.setBackground(droNormalColor);
                savedY = Double.NaN;
            }
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    updateDros();
                }
            });
        }
    };
    
    @SuppressWarnings("serial")
    public Action saveZAction = new AbstractAction(null) {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            if (Double.isNaN(savedZ)) {
                textFieldZ.setBackground(droSavedColor);
                savedZ = getCurrentLocation().getZ();
            }
            else {
                textFieldZ.setBackground(droNormalColor);
                savedZ = Double.NaN;
            }
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    updateDros();
                }
            });
        }
    };
    
    @SuppressWarnings("serial")
    public Action saveCAction = new AbstractAction(null) {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            if (Double.isNaN(savedC)) {
                textFieldC.setBackground(droSavedColor);
                savedC = getCurrentLocation().getRotation();
            }
            else {
                textFieldC.setBackground(droNormalColor);
                savedC = Double.NaN;
            }
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    updateDros();
                }
            });
        }
    };
    
	private MachineListener machineListener = new MachineListener.Adapter() {
		@Override
		public void machineHeadActivity(Machine machine, Head head) {
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					updateDros();
				}
			});
		}

		@Override
		public void machineEnabled(Machine machine) {
			btnStartStop.setAction(machine.isEnabled() ? stopMachineAction : startMachineAction);
			btnStartStop.setForeground(machine.isEnabled() ? stopColor : startColor);
			setEnabled(true);
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					updateDros();
				}
			});
		}

		@Override
		public void machineEnableFailed(Machine machine, String reason) {
			btnStartStop.setAction(machine.isEnabled() ? stopMachineAction : startMachineAction);
			btnStartStop.setForeground(machine.isEnabled() ? stopColor : startColor);
		}

		@Override
		public void machineDisabled(Machine machine, String reason) {
			btnStartStop.setAction(machine.isEnabled() ? stopMachineAction : startMachineAction);
			btnStartStop.setForeground(machine.isEnabled() ? stopColor : startColor);
			setEnabled(false);
		}

		@Override
		public void machineDisableFailed(Machine machine, String reason) {
			btnStartStop.setAction(machine.isEnabled() ? stopMachineAction : startMachineAction);
			btnStartStop.setForeground(machine.isEnabled() ? stopColor : startColor);
		}
	};
	
	private ConfigurationListener configurationListener = new ConfigurationListener.Adapter() {
		@Override
		public void configurationComplete(Configuration configuration) {
		    Machine machine = configuration.getMachine();
			if (machine != null) {
				machine.removeListener(machineListener);
			}
			
			for (Head head : machine.getHeads()) {
	            for (Nozzle nozzle : head.getNozzles()) {
	                comboBoxNozzles.addItem(new NozzleItem(nozzle));
	            }
			}
			setSelectedNozzle(((NozzleItem) comboBoxNozzles.getItemAt(0)).getNozzle());
			
			setUnits(configuration.getSystemUnits());
			machine.addListener(machineListener);
			
			btnStartStop.setAction(machine.isEnabled() ? stopMachineAction : startMachineAction);
			btnStartStop.setForeground(machine.isEnabled() ? stopColor : startColor);

			setEnabled(machine.isEnabled());
		}
	};
}
