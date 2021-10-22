import React, { useState, useEffect } from 'react';
import { connect } from 'react-redux';
import { Field, reduxForm } from 'redux-form';

import { useTranslation } from 'react-i18next';
import Button from '@material-ui/core/Button';
import { MdDelete } from 'react-icons/md';
import { FaEdit } from 'react-icons/fa';
import { ROOM_FORM_TYPE } from '../../constants/reduxForms';

import { CustomDialog } from '../../share/DialogWindows';
import { cardType } from '../../constants/cardType';
import Card from '../../share/Card/Card';
import renderTextField from '../../share/renderedFields/input';
import { deleteTypeService, getOneNewTypeService } from '../../services/roomTypesService';
import './AddNewRoomType.scss';
import {
    SAVE_BUTTON_LABEL,
    ADD_TYPE_LABEL,
    NEW_TYPE_LABEL,
} from '../../constants/translationLabels/formElements';
import { dialogTypes } from '../../constants/dialogs';

let NewRoomType = (props) => {
    const { handleSubmit, pristine, submitting, roomTypes, oneType, initialize } = props;

    const [open, setOpen] = useState(false);
    const [typeId, setTypeId] = useState(-1);

    useEffect(() => {
        let defaultValue = {};
        if (oneType.id) {
            defaultValue = { description: oneType.description, id: oneType.id };
        }
        initialize(defaultValue);
    }, [oneType]);

    const { t } = useTranslation('formElements');

    const handleClickOpen = (id) => {
        setTypeId(id);
        setOpen(true);
    };

    const handleClose = (id) => {
        setOpen(false);
        if (!id) {
            return;
        }
        deleteTypeService(id);
    };

    return (
        <>
            {open && (
                <CustomDialog
                    type={dialogTypes.DELETE_CONFIRM}
                    cardId={typeId}
                    whatDelete={cardType.TYPE.toLowerCase()}
                    open={open}
                    onClose={handleClose}
                />
            )}
            <Card additionClassName="form-card room-form">
                <form className="new-type-container" onSubmit={handleSubmit}>
                    <Field
                        type="text"
                        name="description"
                        component={renderTextField}
                        placeholder={t(ADD_TYPE_LABEL)}
                        label={t(NEW_TYPE_LABEL)}
                        className="form-field"
                        variant="outlined"
                    />
                    <div className="btn-style-wrapper">
                        <Button
                            color="primary"
                            className="btn-style"
                            disabled={pristine || submitting}
                            variant="contained"
                            type="submit"
                        >
                            {t(SAVE_BUTTON_LABEL)}
                        </Button>
                    </div>
                </form>

                <ul className="new-types">
                    {roomTypes.map((roomType) => (
                        <li
                            key={roomType.id}
                            value={roomType.description}
                            className="new-types-list"
                        >
                            <span className="typeDescription">{roomType.description}</span>
                            <span className="buttons">
                                <FaEdit
                                    className="btn edit"
                                    onClick={() => getOneNewTypeService(roomType.id)}
                                />
                                <MdDelete
                                    className="btn delete"
                                    onClick={() => handleClickOpen(roomType.id)}
                                />
                            </span>
                        </li>
                    ))}
                </ul>
            </Card>
        </>
    );
};

const mapStateToProps = (state) => ({
    oneType: state.roomTypes.oneType,
    roomTypes: state.roomTypes.roomTypes,
});

NewRoomType = reduxForm({
    form: ROOM_FORM_TYPE,
})(NewRoomType);

export default connect(mapStateToProps)(NewRoomType);
