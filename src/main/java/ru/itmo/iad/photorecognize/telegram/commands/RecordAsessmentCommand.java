package ru.itmo.iad.photorecognize.telegram.commands;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.User;

import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.itmo.iad.photorecognize.domain.Label;
import ru.itmo.iad.photorecognize.domain.dao.AssessorDao;
import ru.itmo.iad.photorecognize.domain.dto.ImageDto;
import ru.itmo.iad.photorecognize.service.AsessmentSaver;
import ru.itmo.iad.photorecognize.service.AsessorService;
import ru.itmo.iad.photorecognize.service.ImageGetter;
import ru.itmo.iad.photorecognize.telegram.keyboards.ZeroLevelLabelKeyboard;
import ru.itmo.iad.photorecognize.telegram.response.*;

@Service
@Scope("prototype")
@Slf4j
public class RecordAsessmentCommand extends AbsCommand {

	@Autowired
	AsessmentSaver assessmentSaver;

	@Autowired
	AsessorService asessorService;

	@Autowired
	ImageGetter imageGetter;

	@Autowired
	ZeroLevelLabelKeyboard zeroLevelLabelKeyboard;

	private final User user;
	private final String photoId;
	private final Label label;
	private final boolean isHoneypot;
	private final int messageId;

	public RecordAsessmentCommand(User user, String argument, int messageId) throws Exception {
		this.user = user;
		String[] splittedArgument = argument.split(" ", 3);
		this.photoId = splittedArgument[0];
		this.label = Label.getByButtonCode(splittedArgument[1]);
		this.isHoneypot = Boolean.parseBoolean(splittedArgument[2]);
		this.messageId = messageId;
	}

	@Override
	public Response<?> execute() {
		List<Response<?>> responses = new ArrayList<Response<?>>();
		assessmentSaver.saveAssessment(user.getId().toString(), photoId, label, isHoneypot);

		var response = new EditMessageCaptionResponse(
			"Спасибо за идентификацию " + label.getButtonText() + " ✅",
			messageId,
			new InlineKeyboardMarkup(new ArrayList<>())
		);
		responses.add(response);

		try {
			AssessorDao asessor = asessorService.getOrCreateAsessor(user);
			ImageDto image = imageGetter.getImage(asessor.getHoneypotCount());

			responses.add(new PhotoResponse(image.getData(), image.getPhotoId(), null,
					zeroLevelLabelKeyboard.getKeyboard(image.getPhotoId(), image.isHoneypot())));
		} catch (Exception e) {
			log.error("Ошибка!", e);
			responses.add(new StringResponse("Ошибка получения фото!"));
		}

		return new MultiResponse(responses);

	}

}
