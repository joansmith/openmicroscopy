/*
 * org.openmicroscopy.shoola.agents.metadata.view.MetadataViewerComponent 
 *
 *------------------------------------------------------------------------------
 *  Copyright (C) 2006-2008 University of Dundee. All rights reserved.
 *
 *
 * 	This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *------------------------------------------------------------------------------
 */
package org.openmicroscopy.shoola.agents.metadata.view;


//Java imports
import java.awt.Cursor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import javax.swing.JComponent;

//Third-party libraries

//Application-internal dependencies
import org.openmicroscopy.shoola.agents.metadata.browser.Browser;
import org.openmicroscopy.shoola.agents.metadata.browser.TreeBrowserDisplay;
import org.openmicroscopy.shoola.agents.metadata.browser.TreeBrowserSet;
import org.openmicroscopy.shoola.agents.metadata.editor.Editor;
import org.openmicroscopy.shoola.env.data.util.StructuredDataResults;
import org.openmicroscopy.shoola.util.ui.MessageBox;
import org.openmicroscopy.shoola.util.ui.component.AbstractComponent;
import pojos.AnnotationData;
import pojos.DataObject;
import pojos.DatasetData;
import pojos.ExperimenterData;
import pojos.ImageData;
import pojos.PlateData;
import pojos.ProjectData;
import pojos.ScreenData;
import pojos.TagAnnotationData;
import pojos.WellSampleData;

/** 
 * Implements the {@link MetadataViewer} interface to provide the functionality
 * required of the hierarchy viewer component.
 * This class is the component hub and embeds the component's MVC triad.
 * It manages the component's state machine and fires state change 
 * notifications as appropriate, but delegates actual functionality to the
 * MVC sub-components.
 *
 * @author  Jean-Marie Burel &nbsp;&nbsp;&nbsp;&nbsp;
 * <a href="mailto:j.burel@dundee.ac.uk">j.burel@dundee.ac.uk</a>
 * @author Donald MacDonald &nbsp;&nbsp;&nbsp;&nbsp;
 * <a href="mailto:donald@lifesci.dundee.ac.uk">donald@lifesci.dundee.ac.uk</a>
 * @version 3.0
 * <small>
 * (<b>Internal version:</b> $Revision: $Date: $)
 * </small>
 * @since OME3.0
 */
class MetadataViewerComponent 
	extends AbstractComponent
	implements MetadataViewer
{
	
	/** The Model sub-component. */
	private MetadataViewerModel 	model;
	
	/** The Control sub-component. */
	private MetadataViewerControl	controller;
	
	/** The View sub-component. */
	private MetadataViewerUI 		view;

	/**
	 * Initialises a message dialog.
	 * 
	 * @return See above.
	 */
	private MessageBox initMessageDialog()
	{
		MessageBox dialog = new MessageBox(view, "Save Annotations", 
        "Do you want to attach the annotations to: ");
		dialog.setNoText("Cancel");
		dialog.setYesText("OK");
		return dialog;
	}
	
	/**
	 * Creates a new instance.
	 * The {@link #initialize() initialize} method should be called straigh 
	 * after to complete the MVC set up.
	 * 
	 * @param model The Model sub-component. Mustn't be <code>null</code>.
	 */
	MetadataViewerComponent(MetadataViewerModel model)
	{
		if (model == null) throw new NullPointerException("No model.");
		this.model = model;
		controller = new MetadataViewerControl();
		view = new MetadataViewerUI();
	}
	
	/** Links up the MVC triad. */
	void initialize()
	{
		controller.initialize(this, view);
		view.initialize(controller, model);
		if (!(model.getRefObject() instanceof String))
			setSelectionMode(true);
	}

	/** 
	 * Implemented as specified by the {@link MetadataViewer} interface.
	 * @see MetadataViewer#activate(List)
	 */
	public void activate(List channelData)
	{
		switch (model.getState()) {
			case NEW:
				model.getEditor().setChannelsData(channelData, false);
				setRootObject(model.getRefObject());
				break;
			case DISCARDED:
				throw new IllegalStateException(
					"This method can't be invoked in the DISCARDED state.");
		} 
	}

	/** 
	 * Implemented as specified by the {@link MetadataViewer} interface.
	 * @see MetadataViewer#discard()
	 */
	public void discard()
	{
		model.discard();
	}

	/** 
	 * Implemented as specified by the {@link MetadataViewer} interface.
	 * @see MetadataViewer#getState()
	 */
	public int getState() { return model.getState(); }

	/** 
	 * Implemented as specified by the {@link MetadataViewer} interface.
	 * @see MetadataViewer#cancel(TreeBrowserDisplay)
	 */
	public void cancel(TreeBrowserDisplay refNode) { model.cancel(refNode); }

	/** 
	 * Implemented as specified by the {@link MetadataViewer} interface.
	 * @see MetadataViewer#loadMetadata(TreeBrowserDisplay)
	 */
	public void loadMetadata(TreeBrowserDisplay node)
	{
		if (model.getState() == DISCARDED)
			throw new IllegalStateException(
					"This method cannot be invoked in the DISCARDED state.");
		if (node == null)
			throw new IllegalArgumentException("No node specified.");
		Object userObject = node.getUserObject();
		if (userObject instanceof DataObject) {
			if (model.isSingleMode()) {
				model.fireStructuredDataLoading(node);
				fireStateChange();
			}
		} 
	}

	/** 
	 * Implemented as specified by the {@link MetadataViewer} interface.
	 * @see MetadataViewer#setMetadata(TreeBrowserDisplay, Object)
	 */
	public void setMetadata(TreeBrowserDisplay node, Object result)
	{
		if (node == null)
			throw new IllegalArgumentException("No node specified.");
		//
		Object userObject = node.getUserObject();
		Object refObject = model.getRefObject();
		if (refObject == userObject) {
			Browser browser = model.getBrowser();
			if (result instanceof StructuredDataResults) {
				model.setStructuredDataResults((StructuredDataResults) result);
				browser.setParents(node, 
						model.getStructuredData().getParents());
				model.getEditor().setStructuredDataResults();
				view.setOnScreen();
				fireStateChange();
				return;
			}
				
			if (!(userObject instanceof String)) return;
			String name = (String) userObject;
			
			if (browser == null) return;
			if (Browser.DATASETS.equals(name) || Browser.PROJECTS.equals(name)) 
				browser.setParents((TreeBrowserSet) node, (Collection) result);
			model.notifyLoadingEnd(node);
		}
	}

	/** 
	 * Implemented as specified by the {@link MetadataViewer} interface.
	 * @see MetadataViewer#getSelectionUI()
	 */
	public JComponent getSelectionUI()
	{
		if (model.getState() == DISCARDED)
			throw new IllegalStateException("This method cannot be invoked " +
					"in the DISCARDED state.");
		return model.getBrowser().getUI();
	}

	/** 
	 * Implemented as specified by the {@link MetadataViewer} interface.
	 * @see MetadataViewer#getEditorUI()
	 */
	public JComponent getEditorUI()
	{
		if (model.getState() == DISCARDED)
			throw new IllegalStateException("This method cannot be invoked " +
					"in the DISCARDED state.");
		return model.getEditor().getUI();
	}
	
	/** 
	 * Implemented as specified by the {@link MetadataViewer} interface.
	 * @see MetadataViewer#getUI()
	 */
	public JComponent getUI()
	{
		if (model.getState() == DISCARDED)
			throw new IllegalStateException("This method cannot be invoked " +
					"in the DISCARDED state.");
		return view.getUI();
	}
	
	/** 
	 * Implemented as specified by the {@link MetadataViewer} interface.
	 * @see MetadataViewer#setRootObject(Object)
	 */
	public void setRootObject(Object root)
	{
		if (root == null) root = "";
		if (root instanceof WellSampleData) {
			WellSampleData ws = (WellSampleData) root;
			if (ws.getId() < 0) root = null;
		}
		model.setRootObject(root);
		view.setRootObject();
	}

	/** 
	 * Implemented as specified by the {@link MetadataViewer} interface.
	 * @see MetadataViewer#setParentRootObject(Object)
	 */
	public void setParentRootObject(Object parent)
	{
		model.setParentRootObject(parent);
		
	}
	
	/** 
	 * Implemented as specified by the {@link MetadataViewer} interface.
	 * @see MetadataViewer#loadContainers(TreeBrowserDisplay)
	 */
	public void loadContainers(TreeBrowserDisplay node)
	{
		if (node == null)
			throw new IllegalArgumentException("No node specified.");
		model.fireParentLoading((TreeBrowserSet) node);
	}

	/** 
	 * Implemented as specified by the {@link MetadataViewer} interface.
	 * @see MetadataViewer#setContainers(TreeBrowserDisplay, Object)
	 */
	public void setContainers(TreeBrowserDisplay node, Object result)
	{
		Browser browser = model.getBrowser();
		if (node == null) {
			StructuredDataResults data = model.getStructuredData();
			if (data != null) {
				data.setParents((Collection) result);
				browser.setParents(null, (Collection) result);
			}
		} else
			browser.setParents((TreeBrowserSet) node, (Collection) result);
		model.getEditor().setStatus(false);
	}
	
	/** 
	 * Implemented as specified by the {@link MetadataViewer} interface.
	 * @see MetadataViewer#saveData(List, List, List, DataObject)
	 */
	public void saveData(List<AnnotationData> toAdd, 
				List<AnnotationData> toRemove, List<Object> metadata,
				DataObject data)
	{
		if (data == null) return;
		Object refObject = model.getRefObject();
		List<DataObject> toSave = new ArrayList<DataObject>();
		
		if (refObject instanceof ExperimenterData) {
			model.fireExperimenterSaving((ExperimenterData) data);
			return;
		}
		Collection nodes = model.getRelatedNodes();
		Iterator n;
		toSave.add(data);
		if (!model.isSingleMode()) {
			if (nodes != null) {
				n = nodes.iterator();
				while (n.hasNext()) 
					toSave.add((DataObject) n.next());
			}
		}
		
		MessageBox dialog;
		if (refObject instanceof ProjectData) {
			model.fireSaving(toAdd, toRemove, metadata, toSave);
		} else if (refObject instanceof ScreenData) {
			model.fireSaving(toAdd, toRemove, metadata, toSave);
		} else if (refObject instanceof PlateData) {
			model.fireSaving(toAdd, toRemove, metadata, toSave);
			/*
			if ((toAdd.size() == 0 && toRemove.size() == 0)) {
				model.fireSaving(toAdd, toRemove, metadata, toSave);
				return;
			}
			dialog = initMessageDialog();
			JPanel p = new JPanel();
			p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
			ButtonGroup group = new ButtonGroup();
			JRadioButton single = new JRadioButton();
			single.setText("The selected plate");
			single.setSelected(true);
			group.add(single);
			p.add(single);
			JRadioButton batchAnnotation = new JRadioButton();
			group.add(batchAnnotation);
			p.add(batchAnnotation);
			batchAnnotation.setText("All the wells");
			dialog.addBodyComponent(p);
			int option = dialog.centerMsgBox();
			if (option == MessageBox.YES_OPTION) {
				//toSave.add(data);
				if (single.isSelected()) 
					model.fireSaving(toAdd, toRemove, metadata, toSave);
				else
					model.fireBatchSaving(toAdd, toRemove, toSave);
			}
			*/
		} else if (refObject instanceof DatasetData) {
			model.fireSaving(toAdd, toRemove, metadata, toSave);
			//Only update properties.
			/*
			if ((toAdd.size() == 0 && toRemove.size() == 0)) {
				model.fireSaving(toAdd, toRemove, metadata, toSave);
				return;
			}
			dialog = initMessageDialog();
			JPanel p = new JPanel();
			p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
			ButtonGroup group = new ButtonGroup();
			JRadioButton single = new JRadioButton();
			single.setText("The selected dataset");
			single.setSelected(true);
			group.add(single);
			p.add(single);
			JRadioButton batchAnnotation = new JRadioButton();
			group.add(batchAnnotation);
			p.add(batchAnnotation);
			batchAnnotation.setText("The images contained in the " +
					                "selected dataset");
			dialog.addBodyComponent(p);
			int option = dialog.centerMsgBox();
			if (option == MessageBox.YES_OPTION) {
				//toSave.add(data);
				if (single.isSelected()) 
					model.fireSaving(toAdd, toRemove, metadata, toSave);
				else
					model.fireBatchSaving(toAdd, toRemove, toSave);
			}
			*/
		} else if (refObject instanceof ImageData) {
			model.fireSaving(toAdd, toRemove, metadata, toSave);
		} else if (refObject instanceof TagAnnotationData) {
			//Only update properties.
			if ((toAdd.size() == 0 && toRemove.size() == 0)) {
				model.fireSaving(toAdd, toRemove, metadata, toSave);
				return;
			}	
			/*
			TagAnnotationData tag = (TagAnnotationData) refObject;
			Set set = tag.getTags();
			if (set != null) {
				model.fireSaving(toAdd, toRemove, metadata, toSave);
				return;
			}
			set = tag.getDataObjects();
			boolean toAsk = false;
			if (set != null && set.size() > 0) toAsk = true;
			if (!toAsk) {
				model.fireSaving(toAdd, toRemove, metadata, toSave);
				return;
			}
			dialog = initMessageDialog();
			JPanel p = new JPanel();
			p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
			ButtonGroup group = new ButtonGroup();
			JRadioButton single = new JRadioButton();
			single.setText("The selected tag");
			single.setSelected(true);
			group.add(single);
			p.add(single);
			JRadioButton batchAnnotation = new JRadioButton();
			group.add(batchAnnotation);
			p.add(batchAnnotation);
			batchAnnotation.setText("The images linked to the " +
			                       "selected tag");
			dialog.addBodyComponent(p);
			int option = dialog.centerMsgBox();
			if (option == MessageBox.YES_OPTION) {
				//toSave.add(data);
				if (single.isSelected()) 
					model.fireSaving(toAdd, toRemove, metadata, toSave);
				else
					model.fireBatchSaving(toAdd, toRemove, toSave);
			}
			*/
		}
		fireStateChange();
	}
	
	/** 
	 * Implemented as specified by the {@link MetadataViewer} interface.
	 * @see MetadataViewer#hasDataToSave()
	 */
	public boolean hasDataToSave()
	{
		Editor editor = model.getEditor();
		if (editor == null) return false;
		return editor.hasDataToSave();
	}

	/** 
	 * Implemented as specified by the {@link MetadataViewer} interface.
	 * @see MetadataViewer#saveData()
	 */
	public void saveData()
	{
		firePropertyChange(SAVE_DATA_PROPERTY, Boolean.FALSE, Boolean.TRUE);
	}

	/** 
	 * Implemented as specified by the {@link MetadataViewer} interface.
	 * @see MetadataViewer#clearDataToSave()
	 */
	public void clearDataToSave()
	{
		firePropertyChange(CLEAR_SAVE_DATA_PROPERTY, Boolean.FALSE, 
							Boolean.TRUE);
	}

	/** 
	 * Implemented as specified by the {@link MetadataViewer} interface.
	 * @see MetadataViewer#onDataSave(Collection)
	 */
	public void onDataSave(List<DataObject> data)
	{
		if (data == null) return;
		if (model.getState() == DISCARDED) return;
		DataObject dataObject = null;
		if (data.size() == 1) dataObject = data.get(0);
		if (dataObject != null && model.isSameObject(dataObject)) {
			setRootObject(model.getRefObject());
			firePropertyChange(ON_DATA_SAVE_PROPERTY, null, dataObject);
		} else
			firePropertyChange(ON_DATA_SAVE_PROPERTY, null, data);
		model.setState(READY);
		view.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		fireStateChange();
	}

	/** 
	 * Implemented as specified by the {@link MetadataViewer} interface.
	 * @see MetadataViewer#setSelectionMode(boolean)
	 */
	public void setSelectionMode(boolean single)
	{
		model.setSelectionMode(single);
		model.getEditor().setSelectionMode(single);
	}

	/** 
	 * Implemented as specified by the {@link MetadataViewer} interface.
	 * @see MetadataViewer#isSingleMode()
	 */
	public boolean isSingleMode()
	{
		return model.isSingleMode();
	}

	/** 
	 * Implemented as specified by the {@link MetadataViewer} interface.
	 * @see MetadataViewer#setRelatedNodes(Collection)
	 */
	public void setRelatedNodes(Collection nodes)
	{
		model.setRelatedNodes(nodes);
	}

	/** 
	 * Implemented as specified by the {@link MetadataViewer} interface.
	 * @see MetadataViewer#onExperimenterUpdated(ExperimenterData)
	 */
	public void onExperimenterUpdated(ExperimenterData data)
	{
		firePropertyChange(EXPERIMENTER_UPDATED_PROPERTY, null, data);
		setRootObject(data);
	}

	/** 
	 * Implemented as specified by the {@link MetadataViewer} interface.
	 * @see MetadataViewer#loadParents(StructuredDataResults)
	 */
	public void loadParents()
	{
		StructuredDataResults data = model.getStructuredData();
		if (data == null) return;
		if (data.getParents() != null) return;
		Object ho = data.getRelatedObject();
		if (ho != null && ho instanceof DataObject) {
			model.loadParents(ho.getClass(), ((DataObject) ho).getId());
			setStatus(true);
			firePropertyChange(LOADING_PARENTS_PROPERTY, Boolean.FALSE, 
					Boolean.TRUE);
		}
	}

	/** 
	 * Implemented as specified by the {@link MetadataViewer} interface.
	 * @see MetadataViewer#getStructuredData()
	 */
	public StructuredDataResults getStructuredData()
	{
		//TODO: Check state
		return model.getStructuredData();
	}

	/** 
	 * Implemented as specified by the {@link MetadataViewer} interface.
	 * @see MetadataViewer#setStatus(boolean)
	 */
	public void setStatus(boolean busy)
	{
		model.getEditor().setStatus(busy);
	}
	
	/**
	 * Implemented as specified by the {@link MetadataViewer} interface.
	 * @see MetadataViewer#showTagWizard()
	 */
	public void showTagWizard()
	{
		if (model.getState() == DISCARDED) return;
		model.getEditor().loadExistingTags();
		//model.getMetadataViewer().showTagWizard();
	}

	/**
	 * Implemented as specified by the {@link MetadataViewer} interface.
	 * @see MetadataViewer#getObjectPath()
	 */
	public String getObjectPath()
	{
		return model.getRefObjectPath();
	}
	
}
