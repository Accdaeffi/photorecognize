package ru.itmo.iad.photorecognize.service;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;

import com.mongodb.client.gridfs.model.GridFSFile;

import ru.itmo.iad.photorecognize.domain.dao.TrainingImageDao;
import ru.itmo.iad.photorecognize.domain.dto.ImageDto;
import ru.itmo.iad.photorecognize.domain.repository.ImageAsessmentRepository;
import ru.itmo.iad.photorecognize.domain.repository.TrainingImageRepository;

@Service
public class ImageGetter {

	@Autowired
	private TrainingImageRepository trainingImageRepository;

	@Autowired
	private ImageAsessmentRepository imageAsessmentRepository;

	@Autowired
	private GridFsTemplate gridFsTemplate;

	@Autowired
	private GridFsOperations operations;

	private final double LEAST_PICKED_PROCENT = 0.35;

	public ImageDto getImage() throws IllegalStateException, IOException {
		Random r = new Random();

		if (r.nextDouble() < LEAST_PICKED_PROCENT) {
			return getLeastPicked();
		} else {
			return getRandom();
		}
	}

	private ImageDto getLeastPicked() throws IllegalStateException, IOException {
		Map<ObjectId, Integer> imagesAsessmentCount = new HashMap<ObjectId, Integer>();

		trainingImageRepository.findAll().forEach(image -> imagesAsessmentCount.put(image.get_id(), 0));

		imageAsessmentRepository.findAll().stream().forEach(asessment -> {
			imagesAsessmentCount.put(asessment.getImageId(), imagesAsessmentCount.get(asessment.getImageId()) + 1);
		});

		ObjectId minId = null;
		int minimumCount = 1000;

		for (Entry<ObjectId, Integer> entry : imagesAsessmentCount.entrySet()) {
			if (entry.getValue() < minimumCount) {
				minId = entry.getKey();
				minimumCount = entry.getValue();
			}

		}

		TrainingImageDao dao = trainingImageRepository.findById(minId).get();

		return convertDaoToDto(dao);
	}

	private ImageDto getRandom() throws IllegalStateException, IOException {
		List<TrainingImageDao> images = trainingImageRepository.findAll();

		Collections.shuffle(images);

		return convertDaoToDto(images.get(0));
	}

	private ImageDto convertDaoToDto(TrainingImageDao dao) throws IllegalStateException, IOException {
		GridFSFile file = gridFsTemplate.findOne(new Query(Criteria.where("_id").is(dao.getFileId())));

		return new ImageDto(dao.get_id().toHexString(), operations.getResource(file).getInputStream());
	}

}