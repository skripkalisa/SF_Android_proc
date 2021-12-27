package com.example.filmsearch.domain

import com.example.filmsearch.data.API
import com.example.filmsearch.data.MainRepository
import com.example.filmsearch.data.PreferenceProvider
import com.example.filmsearch.data.TmdbApi
import com.example.filmsearch.data.entity.Film
import com.example.filmsearch.data.entity.TmdbResultsDto
import com.example.filmsearch.utils.Converter
import com.example.filmsearch.viewmodel.HomeFragmentViewModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class Interactor(
    private val repo: MainRepository,
    private val retrofitService: TmdbApi,
    private val preferences: PreferenceProvider
) {
    fun getFilmsFromApi(page: Int, callback: HomeFragmentViewModel.ApiCallback) {
        //Метод getDefaultCategoryFromPreferences() будет нам получать при каждом запросе нужный нам список фильмов
        retrofitService.getFilms(getDefaultCategoryFromPreferences(), API.KEY, "en", page)
            .enqueue(object : Callback<TmdbResultsDto> {

                override fun onResponse(
                    call: Call<TmdbResultsDto>,
                    response: Response<TmdbResultsDto>
                ) {
                    //При успехе мы вызываем метод, передаем onSuccess и в этот коллбэк список фильмов
                    val list = Converter.convertApiListToDtoList(response.body()?.tmdbFilms)
                    //Кладем фильмы в бд
                    list.forEach {
                        repo.putToDb(list)
                    }
                    callback.onSuccess(list)

                    callback.onSharedPreferenceChanged(Converter.convertApiListToDtoList(response.body()?.tmdbFilms))

                }
//                override fun onsharedPreferenceChanged(
//                    call: Call<TmdbResultsDto>,
//                    response: Response<TmdbResultsDto>
//                ) {
//                }

                override fun onFailure(call: Call<TmdbResultsDto>, t: Throwable) {
                    //В случае провала вызываем другой метод коллбека
                    callback.onFailure()
                }
            })
    }
    fun getFilmsFromDB(): List<Film> = repo.getAllFromDB()
    //Метод для сохранения настроек
    fun saveDefaultCategoryToPreferences(category: String) {
        preferences.saveDefaultCategory(category)
    }

    //Метод для получения настроек
    fun getDefaultCategoryFromPreferences() = preferences.geDefaultCategory()
}

